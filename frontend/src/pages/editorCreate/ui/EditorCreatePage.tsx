import { authStore } from '@app/providers/auth'
import {
  deleteProject,
  getProjectSvg,
  projectQueryKey,
  projectReferencesStore,
  projectSvgQueryKey,
} from '@entities/project'
import {
  DEFAULT_GENERATION_MODEL,
  GenerationPollingCancelledError,
  GenerationPollingTimeoutError,
  generationProcessStore,
  pollGenerationStatus,
} from '@features/editorGenerate'
import {
  pendingGenerationStore,
  useCreateProjectMutation,
} from '@features/projectCreate'
import { getApiErrorMessage } from '@shared/api/api'
import { useToast } from '@shared/ui'
import { useQueryClient } from '@tanstack/react-query'
import { Footer } from '@widgets/footer'
import { Header } from '@widgets/header'
import { observer } from 'mobx-react-lite'
import { useCallback, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'

import { EditorEmptyState } from './components/EditorEmptyState'
import { EditorGeneratingState } from './components/EditorGeneratingState'
import styles from './EditorCreatePage.module.css'

interface ProjectDraft {
  data?: string
  description: string
  model: string
  name: string
  references?: string[]
}

export const EditorCreatePage = observer(function EditorCreatePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const toast = useToast()
  const { t } = useTranslation()
  const isAuthorized = authStore.isAuthorized
  const { isPending: isCreatingProjectPending, mutateAsync: createProject } =
    useCreateProjectMutation()
  const isGenerationPending =
    generationProcessStore.isInProgress || isCreatingProjectPending
  const generationDraft = generationProcessStore.draft
  const generationAbortControllerRef = useRef<AbortController | null>(null)
  const [cancelledDraft, setCancelledDraft] = useState<ProjectDraft | null>(
    null,
  )

  const startGeneration = useCallback(
    async (draft: ProjectDraft) => {
      setCancelledDraft(null)
      generationProcessStore.begin(draft)
      let pollingController: AbortController | null = null

      const deleteCancelledProject = async (projectId: string | undefined) => {
        if (!projectId) {
          return
        }

        try {
          await deleteProject(projectId)
        } catch (error) {
          console.error('Unable to delete cancelled project', error)
        }
      }

      try {
        const project = await createProject({
          body: draft.data
            ? {
                data: draft.data,
                name: draft.name,
                references: draft.references,
              }
            : {
                model: draft.model,
                name: draft.name,
                prompt: draft.description,
                references:
                  draft.references && draft.references.length > 0
                    ? draft.references
                    : undefined,
              },
        })

        if (!project.id) {
          toast.error(t('Unable to create the project. Please try again.'))
          return
        }

        const jobId = project.job_id

        // Не прерываем запрос создания проекта по AbortSignal: сервер мог уже
        // создать проект, но клиент ещё не получил его id. В этом случае
        // отменить fetch означало бы потерять возможность удалить проект.
        if (generationProcessStore.isCancelling) {
          await deleteCancelledProject(project.id)
          return
        }

        if (!jobId) {
          toast.error(
            t('The generated project is unavailable. Please try again.'),
          )
          return
        }

        generationProcessStore.trackJob(project.id, jobId)
        pollingController = new AbortController()
        generationAbortControllerRef.current = pollingController

        const generatedProject = await pollGenerationStatus(
          queryClient,
          project.id,
          jobId,
          undefined,
          pollingController.signal,
        )

        if (generatedProject.status === 'error') {
          toast.error(
            t('The generated project is unavailable. Please try again.'),
          )
          return
        }

        const generatedProjectUrl = generatedProject.url

        if (!generatedProjectUrl) {
          toast.error(
            t('The generated project is unavailable. Please try again.'),
          )
          return
        }

        queryClient.setQueryData(projectQueryKey(project.id), {
          ...project,
          url: generatedProjectUrl,
        })
        void queryClient.prefetchQuery({
          queryKey: projectSvgQueryKey(project.id, generatedProjectUrl),
          queryFn: () => getProjectSvg(generatedProjectUrl),
        })
        navigate(`/projects/${project.id}`, { viewTransition: true })
      } catch (error) {
        if (error instanceof GenerationPollingCancelledError) {
          await deleteCancelledProject(
            generationProcessStore.projectId ?? undefined,
          )
          return
        }

        toast.error(
          error instanceof GenerationPollingTimeoutError
            ? t('Generation is taking too long. Please try again.')
            : getApiErrorMessage(error),
        )
      } finally {
        if (generationProcessStore.isCancelling) {
          setCancelledDraft(draft)
        }

        if (generationAbortControllerRef.current === pollingController) {
          generationAbortControllerRef.current = null
        }
        generationProcessStore.reset()
      }
    },
    [createProject, navigate, queryClient, t, toast],
  )

  function handleGenerate(draft: ProjectDraft) {
    setCancelledDraft(null)

    if (!isAuthorized) {
      pendingGenerationStore.save(draft.description)
      navigate('/auth', { state: { from: '/' }, viewTransition: true })

      return
    }

    pendingGenerationStore.clear()
    void startGeneration({ ...draft, references: projectReferencesStore.ids })
  }

  function handleUpload(data: string) {
    handleGenerate({
      data,
      description: '',
      model: DEFAULT_GENERATION_MODEL,
      name: t('Untitled project'),
    })
  }

  function handleCancelGeneration() {
    generationProcessStore.cancel()
    generationAbortControllerRef.current?.abort()
  }

  function handleUnauthorizedModelSelect() {
    if (!isAuthorized) {
      navigate('/auth', { viewTransition: true })
    }
  }

  return (
    <div className={styles.page}>
      <Header />
      <main className={styles.content}>
        {isGenerationPending ? (
          <EditorGeneratingState
            description={generationDraft?.description ?? ''}
            isCancelling={generationProcessStore.isCancelling}
            model={generationDraft?.model ?? DEFAULT_GENERATION_MODEL}
            onCancel={handleCancelGeneration}
          />
        ) : (
          <EditorEmptyState
            initialDescription={
              cancelledDraft?.description ?? pendingGenerationStore.prompt ?? ''
            }
            initialModel={cancelledDraft?.model}
            onGenerate={handleGenerate}
            onUpload={handleUpload}
            onUnauthorizedModelSelect={
              isAuthorized ? undefined : handleUnauthorizedModelSelect
            }
          />
        )}
      </main>
      <Footer />
    </div>
  )
})
