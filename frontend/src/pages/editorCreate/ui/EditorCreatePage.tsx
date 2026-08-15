import { authStore } from '@app/providers/auth'
import { useGenerateProjectMutation } from '@features/editorGenerate'
import {
  type ProjectDraft,
  pendingGenerationStore,
  useCreateProjectMutation,
} from '@features/projectCreate'
import { getApiErrorMessage } from '@shared/api/api'
import { useToast } from '@shared/ui'
import { Header } from '@widgets/header'
import { observer } from 'mobx-react-lite'
import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { EditorEmptyState } from './components/EditorEmptyState'
import { EditorGeneratingState } from './components/EditorGeneratingState'
import styles from './EditorCreatePage.module.css'

export const EditorCreatePage = observer(function EditorCreatePage() {
  const navigate = useNavigate()
  const toast = useToast()
  const isAuthorized = authStore.isAuthorized
  const [description, setDescription] = useState('')
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
      setGenerationInProgress(true)

      try {
        const project = await createProject(draft)

        if (!project.id) {
          toast.error('Unable to create the project. Please try again.')
          return
        }

        await generateProject({
          projectId: project.id,
          prompt: draft.description,
        })
        navigate(`/projects/${project.id}`, { viewTransition: true })
      } catch (error) {
        toast.error(getApiErrorMessage(error))
      } finally {
        setGenerationInProgress(false)
      }
    },
    [createProject, generateProject, navigate, toast],
  )

  function handleGenerate(draft: ProjectDraft) {
    if (!isAuthorized) {
      pendingGenerationStore.save(draft)
      navigate('/auth', { viewTransition: true })

      return
    }

    void startGeneration(draft)
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
    <main className={styles.page}>
      <Header />
      {isGenerationPending ? (
        <EditorGeneratingState description={description} />
      ) : (
        <EditorEmptyState onGenerate={handleGenerate} />
      )}
    </main>
  )
})
