import type { EditorDocument, EditorNode } from '@entities/editorDocument'
import type { CanvasTool } from '../model/canvasTool'
import type { ViewportOffset } from '../model/viewportOffset'

import styles from './EditorScene.module.css'
import { SceneFrame } from './sceneFrame/SceneFrame'

interface EditorSceneProps {
  activeTool: CanvasTool
  document: EditorDocument
  isReadOnly: boolean
  onClearSelection: () => void
  onSelectNode: (nodeId: string, isMultiSelect: boolean) => void
  onGroupPositionChange: (nodeId: string, x: number, y: number) => void
  onShapePositionChange: (nodeId: string, x: number, y: number) => void
  onTextPositionChange: (nodeId: string, x: number, y: number) => void
  onViewportOffsetChange: (offset: ViewportOffset) => void
  onZoomChange: (zoom: number) => number
  renderVersion: number
  selectedNode: EditorNode | null
  selectedNodes: EditorNode[]
  viewportOffset: ViewportOffset
  zoom: number
}

export function EditorScene({
  activeTool,
  document,
  isReadOnly,
  onClearSelection,
  onSelectNode,
  onGroupPositionChange,
  onShapePositionChange,
  onTextPositionChange,
  onViewportOffsetChange,
  onZoomChange,
  renderVersion,
  selectedNode,
  selectedNodes,
  viewportOffset,
  zoom,
}: EditorSceneProps) {
  return (
    <section className={styles.scene}>
      <SceneFrame
        activeTool={activeTool}
        document={document}
        isReadOnly={isReadOnly}
        onClearSelection={onClearSelection}
        onGroupPositionChange={onGroupPositionChange}
        onSelectNode={onSelectNode}
        onShapePositionChange={onShapePositionChange}
        onTextPositionChange={onTextPositionChange}
        onViewportOffsetChange={onViewportOffsetChange}
        onZoomChange={onZoomChange}
        renderVersion={renderVersion}
        selectedNode={selectedNode}
        selectedNodes={selectedNodes}
        viewportOffset={viewportOffset}
        zoom={zoom}
      />
    </section>
  )
}
