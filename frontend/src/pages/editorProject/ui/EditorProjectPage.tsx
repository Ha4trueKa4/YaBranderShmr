import { EditorWorkspace } from '@widgets/editorWorkspace'
import { Navigate, useParams } from 'react-router-dom'

import styles from './EditorProjectPage.module.css'

export function EditorProjectPage() {
  const { projectId } = useParams()

  if (!projectId) {
    return <Navigate replace to="/" />
  }

  return (
    <main className={styles.page}>
      {/*
        TODO: вызвать useProjectQuery(projectId), передать загруженный Project в EditorWorkspace
        и инициализировать EditorStore данными project.data. Пока workspace использует editorFixture.
      */}
      <EditorWorkspace />
    </main>
  )
}
