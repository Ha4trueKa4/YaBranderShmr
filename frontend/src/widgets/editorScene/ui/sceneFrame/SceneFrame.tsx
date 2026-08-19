import type { EditorDocument, EditorNode } from '@entities/editorDocument'
import { type RefCallback, useCallback, useRef } from 'react'
import type { CanvasTool } from '../../model/canvasTool'
import { useLayerDrag } from '../../model/useLayerDrag'
import { useScenePan } from '../../model/useScenePan'
import { LayerRenderer } from '../layerRenderer/LayerRenderer'
import { SelectionOverlay } from '../selectionOverlay/SelectionOverlay'
import { TransformHandles } from '../transformHandles/TransformHandles'
import styles from './SceneFrame.module.css'

interface SceneFrameProps {
  activeTool: CanvasTool
  document: EditorDocument
  isReadOnly: boolean
  onSelectNode: (nodeId: string) => void
  onShapePositionChange: (nodeId: string, x: number, y: number) => void
  onTextPositionChange: (nodeId: string, x: number, y: number) => void
  onViewportOffsetChange: (offset: { x: number; y: number }) => void
  renderVersion: number
  selectedNode: EditorNode | null
  viewportOffset: { x: number; y: number }
  zoom: number
}

export function SceneFrame({
  activeTool,
  document,
  isReadOnly,
  onSelectNode,
  onShapePositionChange,
  onTextPositionChange,
  onViewportOffsetChange,
  renderVersion,
  selectedNode,
  viewportOffset,
  zoom,
}: SceneFrameProps) {
  const editableElementsReference = useRef(new Map<string, SVGGElement>())
  const {
    onPointerMove,
    onPointerUp,
    onShapePointerDown,
    onTextPointerDown,
    svgReference,
  } = useLayerDrag({
    activeTool,
    isReadOnly,
    onSelectNode,
    onShapePositionChange,
    onTextPositionChange,
  })
  const {
    onPointerDown: onScenePointerDown,
    onPointerMove: onScenePointerMove,
    onPointerUp: onScenePointerUp,
  } = useScenePan({
    activeTool,
    onViewportOffsetChange,
    viewportOffset,
  })

  const getEditableElement = useCallback(
    (nodeId: string) => editableElementsReference.current.get(nodeId),
    [],
  )

  const registerEditableElement = useCallback(
    (nodeId: string): RefCallback<SVGGElement> =>
      (element) => {
        if (element) {
          editableElementsReference.current.set(nodeId, element)

          return
        }

        editableElementsReference.current.delete(nodeId)
      },
    [],
  )

  return (
    <div className={styles.frame}>
      <svg
        className={
          activeTool === 'hand'
            ? `${styles.canvas} ${styles.handCanvas}`
            : styles.canvas
        }
        ref={svgReference}
        onPointerCancel={onScenePointerUp}
        onPointerDown={onScenePointerDown}
        onPointerMove={onScenePointerMove}
        onPointerUp={onScenePointerUp}
        style={{
          transform: `translate(${viewportOffset.x}px, ${viewportOffset.y}px) scale(${zoom})`,
        }}
        viewBox={`0 0 ${document.width} ${document.height}`}
        xmlns="http://www.w3.org/2000/svg"
      >
        <title>Макет проекта</title>
        <LayerRenderer
          canMoveLayers={activeTool === 'select'}
          document={document}
          onSelectNode={onSelectNode}
          onShapePointerDown={onShapePointerDown}
          onShapePointerMove={onPointerMove}
          onShapePointerUp={onPointerUp}
          onTextPointerDown={onTextPointerDown}
          renderVersion={renderVersion}
          registerEditableElement={registerEditableElement}
        />
        {!isReadOnly && (
          <>
            <SelectionOverlay
              canvasHeight={document.height}
              canvasWidth={document.width}
              getEditableElement={getEditableElement}
              selectedNode={selectedNode}
            />
            <TransformHandles />
          </>
        )}
      </svg>
    </div>
  )
}
