import { authStore } from '@app/providers/auth'
import {
  type ProjectDraft,
  pendingGenerationStore,
  useMockCreateProjectMutation,
} from '@features/projectCreate'
import { Header } from '@widgets/header'
import { observer } from 'mobx-react-lite'
import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { EditorEmptyState } from './components/EditorEmptyState'
import { EditorGeneratingState } from './components/EditorGeneratingState'
import styles from './EditorCreatePage.module.css'

export const EditorCreatePage = observer(function EditorCreatePage() {
  const navigate = useNavigate()
  const isAuthorized = authStore.isAuthorized
  const [description, setDescription] = useState('')

  const handleProjectCreated = useCallback(
    ({ id }: { id: string }) => {
      // TODO: после создания вызывать feature генерации, а затем открывать проект.
      navigate(`/projects/${id}`, { viewTransition: true })
    },
    [navigate],
  )

  const { isPending: isCreatingProjectPending, mutate: createProject } =
    useMockCreateProjectMutation({ onSuccess: handleProjectCreated })

  const startGeneration = useCallback(
    (draft: ProjectDraft) => {
      setDescription(draft.description)
      createProject(draft)
    },
    [createProject],
  )

  function handleGenerate(draft: ProjectDraft) {
    if (!isAuthorized) {
      pendingGenerationStore.save(draft)
      navigate('/auth', { viewTransition: true })

      return
    }

    startGeneration(draft)
  }

  // biome-ignore lint/correctness/useExhaustiveDependencies: isAuthorized is updated by MobX observer.
  useEffect(() => {
    if (!isAuthorized) {
      return
    }

    const draft = pendingGenerationStore.take()

    if (draft) {
      startGeneration(draft)
    }
  }, [isAuthorized, startGeneration])

  return (
    <main className={styles.page}>
      <Header />
      {isCreatingProjectPending ? (
        <EditorGeneratingState description={description} />
      ) : (
        <EditorEmptyState
          description={description}
          onDescriptionChange={setDescription}
          onGenerate={handleGenerate}
        />
      )}
    </main>
  )
})
