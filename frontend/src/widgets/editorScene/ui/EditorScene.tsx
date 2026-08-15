import type { EditorDocument } from '@entities/editorDocument'

import styles from './EditorScene.module.css'
import { SceneFrame } from './sceneFrame/SceneFrame'

interface EditorSceneProps {
  document: EditorDocument
}

export function EditorScene({ document }: EditorSceneProps) {
  return (
    <section className={styles.scene}>
      <SceneFrame document={document} />
    </section>
  )
}
