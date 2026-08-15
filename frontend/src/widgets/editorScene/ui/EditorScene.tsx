import type { EditorDocument } from '@entities/editorDocument'

import styles from './EditorScene.module.css'
import { SceneFrame } from './sceneFrame/SceneFrame'

interface EditorSceneProps {
  document: EditorDocument
  onSelectNode: (nodeId: string) => void
  renderVersion: number
}

export function EditorScene({
  document,
  onSelectNode,
  renderVersion,
}: EditorSceneProps) {
  return (
    <section className={styles.scene}>
      <SceneFrame
        document={document}
        onSelectNode={onSelectNode}
        renderVersion={renderVersion}
      />
    </section>
  )
}
