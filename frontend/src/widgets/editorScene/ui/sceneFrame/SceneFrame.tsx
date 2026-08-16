import type {
  EditorDocument,
  EditorNode,
  EditorShape,
  EditorText,
} from '@entities/editorDocument'
import {
  type PointerEvent as ReactPointerEvent,
  type RefCallback,
  useCallback,
  useRef,
} from 'react'

import { LayerRenderer } from '../layerRenderer/LayerRenderer'
import { SelectionOverlay } from '../selectionOverlay/SelectionOverlay'
import { TransformHandles } from '../transformHandles/TransformHandles'
import styles from './SceneFrame.module.css'

interface SceneFrameProps {
  document: EditorDocument
  isReadOnly: boolean
  onSelectNode: (nodeId: string) => void
  onShapePositionChange: (nodeId: string, x: number, y: number) => void
  onTextPositionChange: (nodeId: string, x: number, y: number) => void
  renderVersion: number
  selectedNode: EditorNode | null
}

type PointerPosition = { x: number; y: number }

type DragState = {
  elementType: 'shape' | 'text'
  nodeId: string
  pointerId: number
  startPointerPosition: PointerPosition
  startTransformPosition: PointerPosition
}

function roundCoordinate(value: number) {
  return Math.round(value * 100) / 100
}

function getSvgPointerPosition(
  svg: SVGSVGElement,
  clientX: number,
  clientY: number,
): PointerPosition | null {
  const screenMatrix = svg.getScreenCTM()

  if (!screenMatrix) {
    return null
  }

  const point = new DOMPoint(clientX, clientY).matrixTransform(
    screenMatrix.inverse(),
  )

  return { x: point.x, y: point.y }
}

export function SceneFrame({
  document,
  isReadOnly,
  onSelectNode,
  onShapePositionChange,
  onTextPositionChange,
  renderVersion,
  selectedNode,
}: SceneFrameProps) {
  const svgReference = useRef<SVGSVGElement>(null)
  const dragStateReference = useRef<DragState | null>(null)
  const editableElementsReference = useRef(new Map<string, SVGGElement>())

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

  const handleShapePointerDown = useCallback(
    (node: EditorShape, event: ReactPointerEvent<SVGGElement>) => {
      if (isReadOnly || event.button !== 0) {
        return
      }

      onSelectNode(node.id)
      const svg = svgReference.current

      if (node.isLocked || !svg) {
        return
      }

      const pointerPosition = getSvgPointerPosition(
        svg,
        event.clientX,
        event.clientY,
      )

      if (!pointerPosition) {
        return
      }

      event.preventDefault()
      event.currentTarget.setPointerCapture(event.pointerId)
      dragStateReference.current = {
        elementType: 'shape',
        nodeId: node.id,
        pointerId: event.pointerId,
        startPointerPosition: pointerPosition,
        startTransformPosition: {
          x: node.transform.x,
          y: node.transform.y,
        },
      }
    },
    [isReadOnly, onSelectNode],
  )

  const handleTextPointerDown = useCallback(
    (node: EditorText, event: ReactPointerEvent<SVGGElement>) => {
      if (isReadOnly || event.button !== 0) {
        return
      }

      onSelectNode(node.id)
      const svg = svgReference.current

      if (node.isLocked || !svg) {
        return
      }

      const pointerPosition = getSvgPointerPosition(
        svg,
        event.clientX,
        event.clientY,
      )

      if (!pointerPosition) {
        return
      }

      event.preventDefault()
      event.currentTarget.setPointerCapture(event.pointerId)
      dragStateReference.current = {
        elementType: 'text',
        nodeId: node.id,
        pointerId: event.pointerId,
        startPointerPosition: pointerPosition,
        startTransformPosition: { x: node.x, y: node.y },
      }
    },
    [isReadOnly, onSelectNode],
  )

  const handleShapePointerMove = useCallback(
    (event: ReactPointerEvent<SVGGElement>) => {
      const dragState = dragStateReference.current
      const svg = svgReference.current

      if (!dragState || dragState.pointerId !== event.pointerId || !svg) {
        return
      }

      const pointerPosition = getSvgPointerPosition(
        svg,
        event.clientX,
        event.clientY,
      )

      if (!pointerPosition) {
        return
      }

      event.preventDefault()
      const x = roundCoordinate(
        dragState.startTransformPosition.x +
          pointerPosition.x -
          dragState.startPointerPosition.x,
      )
      const y = roundCoordinate(
        dragState.startTransformPosition.y +
          pointerPosition.y -
          dragState.startPointerPosition.y,
      )

      if (dragState.elementType === 'shape') {
        onShapePositionChange(dragState.nodeId, x, y)

        return
      }

      onTextPositionChange(dragState.nodeId, x, y)
    },
    [onShapePositionChange, onTextPositionChange],
  )

  const handleShapePointerUp = useCallback(
    (event: ReactPointerEvent<SVGGElement>) => {
      if (dragStateReference.current?.pointerId !== event.pointerId) {
        return
      }

      if (event.currentTarget.hasPointerCapture(event.pointerId)) {
        event.currentTarget.releasePointerCapture(event.pointerId)
      }

      dragStateReference.current = null
    },
    [],
  )

  return (
    <div className={styles.frame}>
      <svg
        className={styles.canvas}
        ref={svgReference}
        viewBox={`0 0 ${document.width} ${document.height}`}
        xmlns="http://www.w3.org/2000/svg"
      >
        <title>Макет проекта</title>
        <LayerRenderer
          document={document}
          onSelectNode={onSelectNode}
          onShapePointerDown={handleShapePointerDown}
          onShapePointerMove={handleShapePointerMove}
          onShapePointerUp={handleShapePointerUp}
          onTextPointerDown={handleTextPointerDown}
          renderVersion={renderVersion}
          registerEditableElement={registerEditableElement}
        />
        {!isReadOnly && (
          <>
            <SelectionOverlay
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
