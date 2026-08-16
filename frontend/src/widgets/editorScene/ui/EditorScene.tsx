import type { EditorDocument, EditorNode } from '@entities/editorDocument'

import styles from './EditorScene.module.css'
import { SceneFrame } from './sceneFrame/SceneFrame'

interface EditorSceneProps {
  document: EditorDocument
  onSelectNode: (nodeId: string) => void
  onShapePositionChange: (nodeId: string, x: number, y: number) => void
  renderVersion: number
  selectedNode: EditorNode | null
}

export function EditorScene({
  document,
  onSelectNode,
  onShapePositionChange,
  renderVersion,
  selectedNode,
}: EditorSceneProps) {
  return (
    <section className={styles.scene}>
      <SceneFrame
        document={document}
        onSelectNode={onSelectNode}
        onShapePositionChange={onShapePositionChange}
        renderVersion={renderVersion}
        selectedNode={selectedNode}
      />
    </section>
  )
}
