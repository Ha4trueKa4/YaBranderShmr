import { authStore } from '@app/providers/auth'
import {
  getProjectSvg,
  projectQueryKey,
  projectSvgQueryKey,
} from '@entities/project'
import {
  cancelGeneration,
  DEFAULT_GENERATION_MODEL,
  GenerationPollingCancelledError,
  GenerationPollingTimeoutError,
  generationProcessStore,
  pollGenerationStatus,
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
import { useCallback, useEffect, useRef, useState } from 'react'
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
      const controller = new AbortController()
      generationAbortControllerRef.current = controller
      generationProcessStore.begin(draft)
      pendingGenerationStore.save(draft)

      const cancelUnfinishedGeneration = async (
        projectId: string | undefined,
        jobId: string | undefined,
      ) => {
        if (!projectId || !jobId) {
          return
        }

        try {
          await cancelGeneration(projectId, jobId)
        } catch (error) {
          console.error('Unable to cancel generation', error)
        }
      }

      try {
        const project = await createProject({
          body: { ...draft, prompt: draft.description },
          signal: controller.signal,
        })

        if (!project.id) {
          toast.error(t('Unable to create the project. Please try again.'))
          return
        }

        const jobId = project.job_id

        if (controller.signal.aborted) {
          await cancelUnfinishedGeneration(project.id, jobId)
          return
        }

        if (!jobId) {
          pendingGenerationStore.take()
          toast.error(
            t('The generated project is unavailable. Please try again.'),
          )
          return
        }

        generationProcessStore.trackJob(project.id, jobId)

        const generatedProject = await pollGenerationStatus(
          queryClient,
          project.id,
          jobId,
          undefined,
          controller.signal,
        )

        if (generatedProject.status === 'error') {
          pendingGenerationStore.take()
          toast.error(
            t('The generated project is unavailable. Please try again.'),
          )
          return
        }

        const generatedProjectUrl = generatedProject.url

        if (!generatedProjectUrl) {
          pendingGenerationStore.take()
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
        pendingGenerationStore.take()
        navigate(`/projects/${project.id}`, { viewTransition: true })
      } catch (error) {
        if (
          controller.signal.aborted ||
          error instanceof GenerationPollingCancelledError
        ) {
          await cancelUnfinishedGeneration(
            generationProcessStore.projectId ?? undefined,
            generationProcessStore.jobId ?? undefined,
          )
          return
        }

        if (!(error instanceof ApiError && error.status === 401)) {
          pendingGenerationStore.take()
        }

        toast.error(
          error instanceof GenerationPollingTimeoutError
            ? t('Generation is taking too long. Please try again.')
            : getApiErrorMessage(error),
        )
      } finally {
        if (controller.signal.aborted) {
          pendingGenerationStore.take()
          setCancelledDraft(draft)
        }

        if (generationAbortControllerRef.current === controller) {
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
      pendingGenerationStore.save(draft)
      navigate('/auth', { viewTransition: true })

      return
    }

    void startGeneration(draft)
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

  // biome-ignore lint/correctness/useExhaustiveDependencies: isAuthorized is updated by MobX observer.
  useEffect(() => {
    if (!isAuthorized || generationProcessStore.isInProgress) {
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
          <EditorGeneratingState
            description={generationDraft?.description ?? ''}
            isCancelling={generationProcessStore.isCancelling}
            model={generationDraft?.model ?? DEFAULT_GENERATION_MODEL}
            onCancel={handleCancelGeneration}
          />
        ) : (
          <EditorEmptyState
            initialDescription={cancelledDraft?.description}
            initialModel={cancelledDraft?.model}
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
