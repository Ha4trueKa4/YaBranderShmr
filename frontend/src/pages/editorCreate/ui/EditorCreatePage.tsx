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
import { useCallback, useEffect, useRef } from 'react'
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
  const {
    isPending: isGeneratingProjectPending,
    mutateAsync: generateProject,
  } = useGenerateProjectMutation()

  const isGenerationPending =
    generationProcessStore.isInProgress ||
    isCreatingProjectPending ||
    isGeneratingProjectPending
  const generationDraft = generationProcessStore.draft
  const generationAbortControllerRef = useRef<AbortController | null>(null)

  const startGeneration = useCallback(
    async (draft: ProjectDraft) => {
      const controller = new AbortController()
      generationAbortControllerRef.current = controller
      generationProcessStore.begin(draft)
      pendingGenerationStore.save(draft)

      let createdProjectId: string | undefined
      let generationStarted = false

      const cancelUnfinishedGeneration = async () => {
        if (!createdProjectId) {
          return
        }

        try {
          await cancelGeneration(createdProjectId)
          queryClient.removeQueries({
            queryKey: projectQueryKey(createdProjectId),
          })
        } catch (error) {
          console.error('Unable to delete unfinished project', error)
        }
      }

      try {
        const project = await createProject({
          body: draft,
          signal: controller.signal,
        })

        if (!project.id) {
          toast.error(t('Unable to create the project. Please try again.'))
          return
        }

        createdProjectId = project.id

        if (controller.signal.aborted) {
          await cancelUnfinishedGeneration()
          return
        }

        const generation = await generateProject({
          model: draft.model,
          projectId: project.id,
          prompt: draft.description,
          signal: controller.signal,
        })
        const jobId = generation.job_id

        if (!jobId) {
          await cancelUnfinishedGeneration()
          pendingGenerationStore.take()
          toast.error(
            t('The generated project is unavailable. Please try again.'),
          )
          return
        }

        generationStarted = true

        generationProcessStore.trackJob(project.id, jobId)

        const generatedProject = await pollGenerationStatus(
          queryClient,
          project.id,
          jobId,
          undefined,
          controller.signal,
        )

        if (generatedProject.status === 'error') {
          await cancelUnfinishedGeneration()
          pendingGenerationStore.take()
          toast.error(
            t('The generated project is unavailable. Please try again.'),
          )
          return
        }

        const generatedProjectUrl = generatedProject.url

        if (!generatedProjectUrl) {
          await cancelUnfinishedGeneration()
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
          await cancelUnfinishedGeneration()
          return
        }

        if (
          createdProjectId &&
          !generationStarted &&
          !(error instanceof ApiError && error.status === 401)
        ) {
          await cancelUnfinishedGeneration()
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
        }

        if (generationAbortControllerRef.current === controller) {
          generationAbortControllerRef.current = null
        }
        generationProcessStore.reset()
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
