import { useNavigate } from 'react-router-dom'

import { useMockCreateProjectMutation } from '@features/projectCreate'
import { EditorEmptyState } from '@widgets/editorEmptyState'
import { EditorGeneratingState } from '@widgets/editorGeneratingState'

import styles from './EditorCreatePage.module.css'

const defaultProject = {
  description: 'Modern abstract technology logo with overlapping gradients.',
  name: 'Untitled project',
}

export function EditorCreatePage() {
  const navigate = useNavigate()
  const {
    isPending: isCreatingProjectPending,
    mutate: createProject,
  } = useMockCreateProjectMutation()

  function handleGenerate() {
    createProject(defaultProject, {
      onSuccess: ({ id }) => navigate(`/projects/${id}`),
    })
  }

  return (
    <main className={styles.page}>
      {isCreatingProjectPending ? (
        <EditorGeneratingState />
      ) : (
        <EditorEmptyState onGenerate={handleGenerate} />
      )}
    </main>
  )
}
