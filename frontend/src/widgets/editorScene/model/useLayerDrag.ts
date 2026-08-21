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
  onDocumentChangeEnd: () => void
  onDocumentChangeStart: () => void
  onGroupPositionChange: (nodeId: string, x: number, y: number) => void
  onSelectNode: (nodeId: string, isMultiSelect: boolean) => void
  onShapePositionChange: (nodeId: string, x: number, y: number) => void
  onTextPositionChange: (nodeId: string, x: number, y: number) => void
  selectedNode: EditorNode | null
  selectedNodes: EditorNode[]
}

type PointerPosition = { x: number; y: number }
type DragTarget = {
  elementType: 'group' | 'shape' | 'text'
  nodeId: string
  startPosition: PointerPosition
}
type DragState = {
  pointerId: number
  startPointerPosition: PointerPosition
  targets: DragTarget[]
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

function hasLockedDescendant(node: EditorGroup): boolean {
  return node.children.some(
    (child) =>
      child.isLocked || (child.type === 'group' && hasLockedDescendant(child)),
  )
}

function toDragTarget(
  node: EditorShape | EditorText | EditorGroup,
): DragTarget {
  if (node.type === 'text') {
    return {
      elementType: 'text',
      nodeId: node.id,
      startPosition: { x: node.x, y: node.y },
    }
  }

  return {
    elementType: node.type,
    nodeId: node.id,
    startPosition: { x: node.transform.x, y: node.transform.y },
  }
}

export function useLayerDrag({
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
}: UseLayerDragParams) {
  const svgReference = useRef<SVGSVGElement>(null)
  const dragStateReference = useRef<DragState | null>(null)

  const startDrag = useCallback(
    (node: EditorShape | EditorText, event: ReactPointerEvent<SVGGElement>) => {
      if (isReadOnly || event.button !== 0 || activeTool !== 'select') {
        return
      }

      if (event.shiftKey) {
        onSelectNode(node.id, true)

        return
      }

      const svg = svgReference.current

      if (!svg) {
        return
      }

      const selectedGroup =
        selectedNode?.type === 'group' &&
        isGroupDescendant(selectedNode, node.id)
          ? selectedNode
          : null

      const selectedGroupToDrag = selectedGroup
      const isSelectedNode = selectedNodes.some(
        (selected) => selected.id === node.id,
      )

      if (
        node.isLocked ||
        selectedGroupToDrag?.isLocked ||
        (selectedGroupToDrag && hasLockedDescendant(selectedGroupToDrag))
      ) {
        return
      }

      const dragTargets = selectedGroupToDrag
        ? [selectedGroupToDrag]
        : isSelectedNode
          ? selectedNodes.filter(
              (selected): selected is EditorShape | EditorText =>
                (selected.type === 'shape' || selected.type === 'text') &&
                !selected.isLocked,
            )
          : [node]

      if (dragTargets.length === 0) {
        return
      }

      if (!selectedGroupToDrag && !isSelectedNode) {
        onSelectNode(node.id, false)
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
        pointerId: event.pointerId,
        startPointerPosition: pointerPosition,
        targets: dragTargets.map(toDragTarget),
      }
      onDocumentChangeStart()
    },
    [
      activeTool,
      isReadOnly,
      onDocumentChangeStart,
      onSelectNode,
      selectedNode,
      selectedNodes,
    ],
  )

  const onShapePointerDown = useCallback(
    (node: EditorShape, event: ReactPointerEvent<SVGGElement>) => {
      startDrag(node, event)
    },
    [startDrag],
  )

  const onTextPointerDown = useCallback(
    (node: EditorText, event: ReactPointerEvent<SVGGElement>) => {
      startDrag(node, event)
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
      const offsetX = pointerPosition.x - dragState.startPointerPosition.x
      const offsetY = pointerPosition.y - dragState.startPointerPosition.y

      for (const target of dragState.targets) {
        const x = roundCoordinate(target.startPosition.x + offsetX)
        const y = roundCoordinate(target.startPosition.y + offsetY)

        if (target.elementType === 'shape') {
          onShapePositionChange(target.nodeId, x, y)
        } else if (target.elementType === 'text') {
          onTextPositionChange(target.nodeId, x, y)
        } else {
          onGroupPositionChange(target.nodeId, x, y)
        }
      }
    },
    [onGroupPositionChange, onShapePositionChange, onTextPositionChange],
  )

  const onPointerUp = useCallback(
    (event: ReactPointerEvent<SVGGElement>) => {
      if (dragStateReference.current?.pointerId !== event.pointerId) {
        return
      }

      if (event.currentTarget.hasPointerCapture(event.pointerId)) {
        event.currentTarget.releasePointerCapture(event.pointerId)
      }

      dragStateReference.current = null
      onDocumentChangeEnd()
    },
    [onDocumentChangeEnd],
  )

  return {
    onPointerMove,
    onPointerUp,
    onShapePointerDown,
    onTextPointerDown,
    svgReference,
  }
}
