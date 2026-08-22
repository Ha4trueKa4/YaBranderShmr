import { authStore } from '@app/providers/auth'
import {
  getProjectSvg,
  projectQueryKey,
  projectSvgQueryKey,
} from '@entities/project'
import {
  DEFAULT_GENERATION_MODEL,
  GenerationPollingTimeoutError,
  pollGenerationStatus,
  useGenerateProjectMutation,
} from '@features/editorGenerate'
import {
  type ProjectDraft,
  pendingGenerationStore,
  useCreateProjectMutation,
} from '@features/projectCreate'
import { ApiError, getApiErrorMessage } from '@shared/api/api'
import { useToast } from '@shared/ui'
import { useQueryClient } from '@tanstack/react-query'
import { Footer } from '@widgets/footer'
import { Header } from '@widgets/header'
import { observer } from 'mobx-react-lite'
import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'

import { EditorEmptyState } from './components/EditorEmptyState'
import { EditorGeneratingState } from './components/EditorGeneratingState'
import styles from './EditorCreatePage.module.css'

export const EditorCreatePage = observer(function EditorCreatePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const toast = useToast()
  const { t } = useTranslation()
  const isAuthorized = authStore.isAuthorized
  const [description, setDescription] = useState('')
  const [model, setModel] = useState(DEFAULT_GENERATION_MODEL)
  const [isGenerationInProgress, setGenerationInProgress] = useState(false)
  const { isPending: isCreatingProjectPending, mutateAsync: createProject } =
    useCreateProjectMutation()
  const {
    isPending: isGeneratingProjectPending,
    mutateAsync: generateProject,
  } = useGenerateProjectMutation()

  const isGenerationPending =
    isCreatingProjectPending ||
    isGeneratingProjectPending ||
    isGenerationInProgress

  const startGeneration = useCallback(
    async (draft: ProjectDraft) => {
      setDescription(draft.description)
      setModel(draft.model)
      setGenerationInProgress(true)
      pendingGenerationStore.save(draft)

      try {
        const project = await createProject(draft)

        if (!project.id) {
          toast.error(t('Unable to create the project. Please try again.'))
          return
        }

        const generation = await generateProject({
          model: draft.model,
          projectId: project.id,
          prompt: draft.description,
        })
        const jobId = generation.job_id

        if (!jobId) {
          toast.error(
            t('The generated project is unavailable. Please try again.'),
          )
          return
        }

        const generatedProject = await pollGenerationStatus(
          queryClient,
          project.id,
          jobId,
        )

        if (generatedProject.status === 'error') {
          toast.error(
            generatedProject.message ??
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
        await queryClient.fetchQuery({
          queryKey: projectSvgQueryKey(project.id, generatedProjectUrl),
          queryFn: () => getProjectSvg(generatedProjectUrl),
        })
        pendingGenerationStore.take()
        navigate(`/projects/${project.id}`, { viewTransition: true })
      } catch (error) {
        if (!(error instanceof ApiError && error.status === 401)) {
          pendingGenerationStore.take()
        }

        toast.error(
          error instanceof GenerationPollingTimeoutError
            ? t('Generation is taking too long. Please try again.')
            : getApiErrorMessage(error),
        )
      } finally {
        setGenerationInProgress(false)
      }
    },
    [createProject, generateProject, navigate, queryClient, t, toast],
  )

  function handleGenerate(draft: ProjectDraft) {
    if (!isAuthorized) {
      pendingGenerationStore.save(draft)
      navigate('/auth', { viewTransition: true })

      return
    }

    void startGeneration(draft)
  }

  function handleUnauthorizedModelSelect() {
    if (!isAuthorized) {
      navigate('/auth', { viewTransition: true })
    }
  }

  // biome-ignore lint/correctness/useExhaustiveDependencies: isAuthorized is updated by MobX observer.
  useEffect(() => {
    if (!isAuthorized) {
      return
    }

    const draft = pendingGenerationStore.take()

    if (draft) {
      void startGeneration(draft)
    }
  }, [isAuthorized, startGeneration])

  return (
    <div className={styles.page}>
      <Header />
      <main className={styles.content}>
        {isGenerationPending ? (
          <EditorGeneratingState description={description} model={model} />
        ) : (
          <EditorEmptyState
            onGenerate={handleGenerate}
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
