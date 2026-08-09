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
    // Сюда подставить значения формы: название и описание вместо `defaultProject`.
    // После создания проект должен быть передан в feature генерации.
    createProject(defaultProject, {
      onSuccess: ({ id }) => navigate(`/projects/${id}`),
    })
  }

  return (
    <main className={styles.page}>
      {/* Ошибку мутации и повторную отправку добавить рядом с этим переключением состояния. */}
      {isCreatingProjectPending ? (
        <EditorGeneratingState />
      ) : (
        <EditorEmptyState onGenerate={handleGenerate} />
      )}
    </main>
  )
}
