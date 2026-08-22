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
  onMeasuredRotationCenterChange: (
    center: { nodeId: string; x: number; y: number } | null,
  ) => void
  onShapePositionChange: (nodeId: string, x: number, y: number) => void
  onShapeTransformChange: (
    nodeId: string,
    transform: {
      rotationCenter?: { x: number; y: number }
      scaleX: number
      scaleY: number
      x: number
      y: number
    },
  ) => void
  onShapeRotationChange: (
    nodeId: string,
    rotation: number,
    rotationCenter: { x: number; y: number },
  ) => void
  onTextPositionChange: (nodeId: string, x: number, y: number) => void
  onTextTransformChange: (
    nodeId: string,
    transform: {
      rotationCenter?: { x: number; y: number }
      scaleX: number
      scaleY: number
      x: number
      y: number
    },
  ) => void
  onTextRotationChange: (
    nodeId: string,
    rotation: number,
    rotationCenter: { x: number; y: number },
  ) => void
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
  onMeasuredRotationCenterChange,
  onShapePositionChange,
  onShapeTransformChange,
  onShapeRotationChange,
  onTextPositionChange,
  onTextTransformChange,
  onTextRotationChange,
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
        onMeasuredRotationCenterChange={onMeasuredRotationCenterChange}
        onSelectNode={onSelectNode}
        onShapePositionChange={onShapePositionChange}
        onShapeTransformChange={onShapeTransformChange}
        onShapeRotationChange={onShapeRotationChange}
        onTextPositionChange={onTextPositionChange}
        onTextRotationChange={onTextRotationChange}
        onTextTransformChange={onTextTransformChange}
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
