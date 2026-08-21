import type { EditorDocument, EditorNode } from '@entities/editorDocument'
import {
  type PointerEvent as ReactPointerEvent,
  type RefCallback,
  useCallback,
  useRef,
} from 'react'
import type { CanvasTool } from '../../model/canvasTool'
import { constrainViewportOffset } from '../../model/constrainViewportOffset'
import { useCanvasGesture } from '../../model/useCanvasGesture'
import { useCanvasPan } from '../../model/useCanvasPan'
import { useCanvasWheel } from '../../model/useCanvasWheel'
import { useLayerDrag } from '../../model/useLayerDrag'
import type { ViewportOffset } from '../../model/viewportOffset'
import { LayerRenderer } from '../layerRenderer/LayerRenderer'
import { SelectionOverlay } from '../selectionOverlay/SelectionOverlay'
import { TransformHandles } from '../transformHandles/TransformHandles'
import styles from './SceneFrame.module.css'

interface SceneFrameProps {
  activeTool: CanvasTool
  constrainZoom: (zoom: number) => number
  document: EditorDocument
  isReadOnly: boolean
  onClearSelection: () => void
  onDocumentChangeEnd: () => void
  onDocumentChangeStart: () => void
  onGroupPositionChange: (nodeId: string, x: number, y: number) => void
  onSelectNode: (nodeId: string, isMultiSelect: boolean) => void
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

export function SceneFrame({
  activeTool,
  constrainZoom,
  document,
  isReadOnly,
  onClearSelection,
  onDocumentChangeEnd,
  onDocumentChangeStart,
  onGroupPositionChange,
  onSelectNode,
  onShapePositionChange,
  onTextPositionChange,
  onViewportOffsetChange,
  onZoomChange,
  renderVersion,
  selectedNode,
  selectedNodes,
  viewportOffset,
  zoom,
}: SceneFrameProps) {
  const editableElementsReference = useRef(new Map<string, SVGGElement>())
  const frameReference = useRef<HTMLDivElement>(null)
  const isNativeGestureActiveReference = useRef(false)
  const viewportReference = useRef<HTMLDivElement>(null)
  const getEditableElement = useCallback(
    (nodeId: string) => editableElementsReference.current.get(nodeId),
    [],
  )
  const {
    onPointerMove,
    onPointerUp,
    onShapePointerDown,
    onTextPointerDown,
    svgReference,
  } = useLayerDrag({
    activeTool,
    isReadOnly,
    onDocumentChangeEnd,
    onDocumentChangeStart,
    onGroupPositionChange,
    onSelectNode,
    onShapePositionChange,
    onTextPositionChange,
    selectedNode,
    selectedNodes,
  })

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
  const constrainOffset = useCallback(
    (offset: ViewportOffset, nextZoom = zoom) => {
      const frame = frameReference.current
      const viewport = viewportReference.current

      if (!frame || !viewport) {
        return offset
      }

      return constrainViewportOffset(
        offset,
        { height: viewport.clientHeight, width: viewport.clientWidth },
        { height: frame.clientHeight, width: frame.clientWidth },
        nextZoom,
      )
    },
    [zoom],
  )
  const canvasPanHandlers = useCanvasPan({
    activeTool,
    constrainOffset,
    offset: viewportOffset,
    onOffsetChange: onViewportOffsetChange,
  })
  useCanvasWheel({
    constrainOffset,
    constrainZoom,
    isNativeGestureActiveReference,
    offset: viewportOffset,
    onOffsetChange: onViewportOffsetChange,
    onZoomChange,
    viewportReference,
    zoom,
  })
  useCanvasGesture({
    constrainOffset,
    isNativeGestureActiveReference,
    offset: viewportOffset,
    onOffsetChange: onViewportOffsetChange,
    onZoomChange,
    viewportReference,
    zoom,
  })

  function handleViewportPointerDownCapture(
    event: ReactPointerEvent<HTMLDivElement>,
  ) {
    if (event.button !== 0) {
      return
    }

    const target = event.target

    if (target instanceof Element && target.closest('[data-editor-node-id]')) {
      return
    }

    onClearSelection()
  }

  return (
    <div
      className={
        activeTool === 'hand'
          ? `${styles.viewport} ${styles.handTool}`
          : styles.viewport
      }
      onPointerDownCapture={handleViewportPointerDownCapture}
      ref={viewportReference}
      {...canvasPanHandlers}
    >
      <div
        className={styles.world}
        style={{
          transform: `translate(${viewportOffset.x}px, ${viewportOffset.y}px) scale(${zoom})`,
        }}
      >
        <div className={styles.frame} ref={frameReference}>
          {/* biome-ignore lint/a11y/noSvgWithoutTitle: The interactive editor canvas has no textual equivalent. */}
          <svg
            className={styles.canvas}
            ref={svgReference}
            viewBox={`${document.viewBoxX ?? 0} ${document.viewBoxY ?? 0} ${document.width} ${document.height}`}
            xmlns="http://www.w3.org/2000/svg"
          >
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
                  canvasX={document.viewBoxX ?? 0}
                  canvasY={document.viewBoxY ?? 0}
                  getEditableElement={getEditableElement}
                  selectedNodes={selectedNodes}
                />
                <TransformHandles />
              </>
            )}
          </svg>
        </div>
      </div>
    </div>
  )
}
