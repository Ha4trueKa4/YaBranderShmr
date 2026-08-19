import type {
  EditorGroup,
  EditorNode,
  EditorShape,
  EditorText,
} from '@entities/editorDocument'
import {
  type PointerEvent as ReactPointerEvent,
  useCallback,
  useRef,
} from 'react'
import type { CanvasTool } from './canvasTool'

interface UseLayerDragParams {
  activeTool: CanvasTool
  isReadOnly: boolean
  onGroupPositionChange: (nodeId: string, x: number, y: number) => void
  onSelectNode: (nodeId: string) => void
  onShapePositionChange: (nodeId: string, x: number, y: number) => void
  onTextPositionChange: (nodeId: string, x: number, y: number) => void
  selectedNode: EditorNode | null
}

type PointerPosition = { x: number; y: number }

type DragState = {
  elementType: 'group' | 'shape' | 'text'
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

function isGroupDescendant(group: EditorGroup, nodeId: string): boolean {
  return group.children.some(
    (child) =>
      child.id === nodeId ||
      (child.type === 'group' && isGroupDescendant(child, nodeId)),
  )
}

export function useLayerDrag({
  activeTool,
  isReadOnly,
  onGroupPositionChange,
  onSelectNode,
  onShapePositionChange,
  onTextPositionChange,
  selectedNode,
}: UseLayerDragParams) {
  const svgReference = useRef<SVGSVGElement>(null)
  const dragStateReference = useRef<DragState | null>(null)

  const startDrag = useCallback(
    (
      node: EditorShape | EditorText,
      elementType: DragState['elementType'],
      startTransformPosition: PointerPosition,
      event: ReactPointerEvent<SVGGElement>,
    ) => {
      if (isReadOnly || event.button !== 0) {
        return
      }

      const svg = svgReference.current

      if (activeTool !== 'select' || !svg) {
        return
      }

      const selectedGroup =
        selectedNode?.type === 'group' &&
        isGroupDescendant(selectedNode, node.id)
          ? selectedNode
          : null
      const dragTarget = selectedGroup ?? node

      if (dragTarget.isLocked) {
        return
      }

      if (!selectedGroup) {
        onSelectNode(node.id)
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
        elementType: selectedGroup ? 'group' : elementType,
        nodeId: dragTarget.id,
        pointerId: event.pointerId,
        startPointerPosition: pointerPosition,
        startTransformPosition: selectedGroup
          ? { x: selectedGroup.transform.x, y: selectedGroup.transform.y }
          : startTransformPosition,
      }
    },
    [activeTool, isReadOnly, onSelectNode, selectedNode],
  )

  const onShapePointerDown = useCallback(
    (node: EditorShape, event: ReactPointerEvent<SVGGElement>) => {
      startDrag(
        node,
        'shape',
        { x: node.transform.x, y: node.transform.y },
        event,
      )
    },
    [startDrag],
  )

  const onTextPointerDown = useCallback(
    (node: EditorText, event: ReactPointerEvent<SVGGElement>) => {
      startDrag(node, 'text', { x: node.x, y: node.y }, event)
    },
    [startDrag],
  )

  const onPointerMove = useCallback(
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

      if (dragState.elementType === 'group') {
        onGroupPositionChange(dragState.nodeId, x, y)

        return
      }

      onTextPositionChange(dragState.nodeId, x, y)
    },
    [onGroupPositionChange, onShapePositionChange, onTextPositionChange],
  )

  const onPointerUp = useCallback((event: ReactPointerEvent<SVGGElement>) => {
    if (dragStateReference.current?.pointerId !== event.pointerId) {
      return
    }

    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId)
    }

    dragStateReference.current = null
  }, [])

  return {
    onPointerMove,
    onPointerUp,
    onShapePointerDown,
    onTextPointerDown,
    svgReference,
  }
}
