import type { EditorDocument } from '@entities/editorDocument'

import styles from './EditorScene.module.css'
import { SceneFrame } from './sceneFrame/SceneFrame'

interface EditorSceneProps {
  document: EditorDocument
  onSelectNode: (nodeId: string) => void
}

export function EditorScene({ document, onSelectNode }: EditorSceneProps) {
  return (
    <section className={styles.scene}>
      <SceneFrame document={document} onSelectNode={onSelectNode} />
    </section>
  )
}
