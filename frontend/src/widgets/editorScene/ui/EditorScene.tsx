import type { EditorDocument, EditorNode } from '@entities/editorDocument'
import type { CanvasTool } from '../model/canvasTool'
import type { ViewportOffset } from '../model/viewportOffset'

import styles from './EditorScene.module.css'
import { SceneFrame } from './sceneFrame/SceneFrame'

interface EditorSceneProps {
  activeTool: CanvasTool
  constrainZoom: (zoom: number) => number
  document: EditorDocument
  isReadOnly: boolean
  onClearSelection: () => void
  onDocumentChangeEnd: () => void
  onDocumentChangeStart: () => void
  onSelectNode: (nodeId: string, isMultiSelect: boolean) => void
  onGroupPositionChange: (nodeId: string, x: number, y: number) => void
  onRawPositionChange: (nodeId: string, x: number, y: number) => void
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
  constrainZoom,
  document,
  isReadOnly,
  onClearSelection,
  onDocumentChangeEnd,
  onDocumentChangeStart,
  onSelectNode,
  onGroupPositionChange,
  onRawPositionChange,
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
        constrainZoom={constrainZoom}
        document={document}
        isReadOnly={isReadOnly}
        onClearSelection={onClearSelection}
        onDocumentChangeEnd={onDocumentChangeEnd}
        onDocumentChangeStart={onDocumentChangeStart}
        onGroupPositionChange={onGroupPositionChange}
        onRawPositionChange={onRawPositionChange}
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
