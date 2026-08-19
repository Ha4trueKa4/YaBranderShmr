import type { EditorDocument, EditorNode } from '@entities/editorDocument'
import type { CanvasTool } from '../model/canvasTool'

import styles from './EditorScene.module.css'
import { SceneFrame } from './sceneFrame/SceneFrame'

interface EditorSceneProps {
  activeTool: CanvasTool
  document: EditorDocument
  isReadOnly: boolean
  onSelectNode: (nodeId: string) => void
  onShapePositionChange: (nodeId: string, x: number, y: number) => void
  onTextPositionChange: (nodeId: string, x: number, y: number) => void
  renderVersion: number
  selectedNode: EditorNode | null
  zoom: number
}

export function EditorScene({
  activeTool,
  document,
  isReadOnly,
  onSelectNode,
  onShapePositionChange,
  onTextPositionChange,
  renderVersion,
  selectedNode,
  zoom,
}: EditorSceneProps) {
  return (
    <section className={styles.scene}>
      <SceneFrame
        activeTool={activeTool}
        document={document}
        isReadOnly={isReadOnly}
        onSelectNode={onSelectNode}
        onShapePositionChange={onShapePositionChange}
        onTextPositionChange={onTextPositionChange}
        renderVersion={renderVersion}
        selectedNode={selectedNode}
        zoom={zoom}
      />
    </section>
  )
}
