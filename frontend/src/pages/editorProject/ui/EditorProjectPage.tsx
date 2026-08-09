import { Navigate, useParams } from 'react-router-dom'

import { EditorWorkspace } from '@widgets/editorWorkspace'

import styles from './EditorProjectPage.module.css'

export function EditorProjectPage() {
  const { projectId } = useParams()

  if (!projectId) {
    return <Navigate replace to="/" />
  }

  return (
    <main className={styles.page}>
      {/* Загрузить проект по projectId через query и передать данные в EditorWorkspace. */}
      <EditorWorkspace />
    </main>
  )
}
