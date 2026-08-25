import { type EditorDocument, importSvg } from '@entities/editorDocument'
import {
  getProjectSvg,
  projectQueryKey,
  projectSvgQueryPrefix,
} from '@entities/project'
import {
  type ProjectChatMessage,
  projectChatQueryKey,
  useProjectChatQuery,
} from '@entities/projectChat'
import {
  getDefaultChatGenerationModel,
  getGenerationErrorMessage,
  pollGenerationStatus,
  useGenerateProjectMutation,
  useModelsQuery,
} from '@features/editorGenerate'
import { useToast } from '@shared/ui'
import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useState } from 'react'

interface UseEditorChatGenerationParams {
  canEdit: boolean
  hasUnsavedChanges: boolean
  onDocumentGenerated: (document: EditorDocument) => void
  projectId: string
  saveVersion: () => Promise<boolean>
}

function createTransientMessage(
  kind: ProjectChatMessage['kind'],
  jobId?: string,
): ProjectChatMessage {
  const createdAt = new Date().toISOString()

  return {
    createdAt,
    id: `transient:${kind}:${jobId ?? createdAt}`,
    jobId,
    kind,
    role: kind === 'prompt' ? 'user' : 'assistant',
  }
}

export function useEditorChatGeneration({
  canEdit,
  hasUnsavedChanges,
  onDocumentGenerated,
  projectId,
  saveVersion,
}: UseEditorChatGenerationParams) {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [isPolling, setIsPolling] = useState(false)
  const { isPending: isStartingGeneration, mutateAsync: generateProject } =
    useGenerateProjectMutation()
  const { data: models = [] } = useModelsQuery()
  const chatQuery = useProjectChatQuery(projectId, canEdit)
  const messages = chatQuery.data ?? []
  const hasPendingChatMessage = messages.some(
    (message) => message.kind === 'pending' || message.kind === 'cancelling',
  )
  const isGenerationPending =
    isStartingGeneration || isPolling || hasPendingChatMessage

  const appendTransientMessage = useCallback(
    (message: ProjectChatMessage) => {
      queryClient.setQueryData<ProjectChatMessage[]>(
        projectChatQueryKey(projectId),
        (currentMessages = []) => [...currentMessages, message],
      )
    },
    [projectId, queryClient],
  )

  const sendPrompt = useCallback(
    async (prompt: string) => {
      if (!canEdit || isGenerationPending) {
        return
      }

      if (hasUnsavedChanges && !(await saveVersion())) {
        return
      }

      setIsPolling(true)

      try {
        const generation = await generateProject({
          model: getDefaultChatGenerationModel(models),
          projectId,
          prompt,
        })
        const jobId = generation.job_id

        if (!jobId) {
          throw new TypeError('Generation job ID is missing')
        }

        appendTransientMessage({
          ...createTransientMessage('prompt', jobId),
          text: prompt,
        })
        appendTransientMessage(createTransientMessage('pending', jobId))

        const generationResult = await pollGenerationStatus(
          queryClient,
          projectId,
          jobId,
        )

        if (generationResult.status !== 'done' || !generationResult.url) {
          appendTransientMessage(createTransientMessage('error', jobId))
          toast.error(
            getGenerationErrorMessage(
              generationResult.message,
              'Unable to update the design. Please try again.',
            ),
          )
          await chatQuery.refetch()

          return
        }

        const generatedSvg = await getProjectSvg(generationResult.url)
        const generatedDocument = importSvg(generatedSvg).document

        onDocumentGenerated(generatedDocument)
        appendTransientMessage(createTransientMessage('success', jobId))

        await Promise.all([
          queryClient.invalidateQueries({
            queryKey: projectQueryKey(projectId),
          }),
          queryClient.invalidateQueries({
            queryKey: projectSvgQueryPrefix(projectId),
          }),
          queryClient.invalidateQueries({
            queryKey: ['project-versions', projectId],
          }),
          chatQuery.refetch(),
        ])
      } catch (error) {
        appendTransientMessage(createTransientMessage('error'))
        toast.error(getGenerationErrorMessage(error))
      } finally {
        setIsPolling(false)
      }
    },
    [
      appendTransientMessage,
      canEdit,
      chatQuery,
      generateProject,
      hasUnsavedChanges,
      isGenerationPending,
      onDocumentGenerated,
      projectId,
      queryClient,
      saveVersion,
      toast,
      models,
    ],
  )

  return {
    isGenerationPending,
    messages,
    sendPrompt,
  }
}
