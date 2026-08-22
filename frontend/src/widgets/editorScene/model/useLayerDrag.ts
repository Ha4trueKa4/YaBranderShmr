import type {
  EditorGroup,
  EditorNode,
  EditorRawNode,
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
  document: EditorNode[]
  getEditableElement: (nodeId: string) => SVGGElement | undefined
  isReadOnly: boolean
  onDocumentChangeEnd: () => void
  onDocumentChangeStart: () => void
  onGroupPositionChange: (nodeId: string, x: number, y: number) => void
  onRawPositionChange: (nodeId: string, x: number, y: number) => void
  onSelectNode: (nodeId: string, isMultiSelect: boolean) => void
  onShapePositionChange: (nodeId: string, x: number, y: number) => void
  onTextPositionChange: (nodeId: string, x: number, y: number) => void
  selectedNode: EditorNode | null
  selectedNodes: EditorNode[]
}

type PointerPosition = { x: number; y: number }
type DragTarget = {
  elementType: 'group' | 'raw' | 'shape' | 'text'
  nodeId: string
  screenToLocalMatrix: DOMMatrix
  startPosition: PointerPosition
  startPointerPosition: PointerPosition
}
type DragState = {
  hitNodeId: string
  pointerId: number
  startClientPosition: PointerPosition
  targets: DragTarget[]
}

type TapState = {
  clientX: number
  clientY: number
  nodeId: string
  timestamp: number
}

const DOUBLE_TAP_MAX_DELAY = 300
const DOUBLE_TAP_MAX_DISTANCE = 20
const TAP_MAX_DISTANCE = 8

function roundCoordinate(value: number) {
  return Math.round(value * 100) / 100
}

function getLocalPointerPosition(
  screenToLocalMatrix: DOMMatrix,
  clientX: number,
  clientY: number,
): PointerPosition {
  const point = new DOMPoint(clientX, clientY).matrixTransform(
    screenToLocalMatrix,
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
  node: EditorShape | EditorText | EditorGroup | EditorRawNode,
  element: SVGGElement,
  clientX: number,
  clientY: number,
): DragTarget | null {
  const screenMatrix = element.getScreenCTM()

  if (!screenMatrix) {
    return null
  }

  const screenToLocalMatrix = screenMatrix.inverse()

  if (node.type === 'text') {
    return {
      elementType: 'text',
      nodeId: node.id,
      screenToLocalMatrix,
      startPosition: { x: node.x, y: node.y },
      startPointerPosition: getLocalPointerPosition(
        screenToLocalMatrix,
        clientX,
        clientY,
      ),
    }
  }

  return {
    elementType: node.type,
    nodeId: node.id,
    screenToLocalMatrix,
    startPosition: { x: node.transform.x, y: node.transform.y },
    startPointerPosition: getLocalPointerPosition(
      screenToLocalMatrix,
      clientX,
      clientY,
    ),
  }
}

function isPointerWithinElement(
  element: SVGGElement | undefined,
  clientX: number,
  clientY: number,
) {
  if (!element) {
    return false
  }

  const bounds = element.getBoundingClientRect()

  return (
    clientX >= bounds.left &&
    clientX <= bounds.right &&
    clientY >= bounds.top &&
    clientY <= bounds.bottom
  )
}

function isPointerWithinNode(
  node: EditorNode,
  getEditableElement: (nodeId: string) => SVGGElement | undefined,
  clientX: number,
  clientY: number,
): boolean {
  if (node.type === 'group') {
    return node.children.some((child) =>
      isPointerWithinNode(child, getEditableElement, clientX, clientY),
    )
  }

  return isPointerWithinElement(getEditableElement(node.id), clientX, clientY)
}

function getVisibleLeafNodes(nodes: EditorNode[]): EditorNode[] {
  return nodes.flatMap((node) => {
    if (!node.isVisible) {
      return []
    }

    return node.type === 'group' ? getVisibleLeafNodes(node.children) : [node]
  })
}

function getOverlappingNodeIds(
  nodes: EditorNode[],
  getEditableElement: (nodeId: string) => SVGGElement | undefined,
  clientX: number,
  clientY: number,
) {
  return getVisibleLeafNodes(nodes)
    .filter(
      (node) =>
        (node.type === 'raw' || !node.isLocked) &&
        isPointerWithinElement(getEditableElement(node.id), clientX, clientY),
    )
    .map((node) => node.id)
    .reverse()
}

export function useLayerDrag({
  activeTool,
  document,
  getEditableElement,
  isReadOnly,
  onDocumentChangeEnd,
  onDocumentChangeStart,
  onGroupPositionChange,
  onRawPositionChange,
  onSelectNode,
  onShapePositionChange,
  onTextPositionChange,
  selectedNode,
  selectedNodes,
}: UseLayerDragParams) {
  const svgReference = useRef<SVGSVGElement>(null)
  const dragStateReference = useRef<DragState | null>(null)
  const lastTapReference = useRef<TapState | null>(null)

  const startDrag = useCallback(
    (
      node: EditorShape | EditorText | EditorRawNode,
      event: ReactPointerEvent<SVGGElement>,
    ) => {
      if (isReadOnly || event.button !== 0 || activeTool !== 'select') {
        return
      }

      if (event.shiftKey) {
        onSelectNode(node.id, true)

        return
      }

      const lastTap = lastTapReference.current
      const horizontalOffset = lastTap ? event.clientX - lastTap.clientX : 0
      const verticalOffset = lastTap ? event.clientY - lastTap.clientY : 0
      const isDoubleTap =
        lastTap &&
        lastTap.nodeId === node.id &&
        event.timeStamp - lastTap.timestamp <= DOUBLE_TAP_MAX_DELAY &&
        Math.hypot(horizontalOffset, verticalOffset) <= DOUBLE_TAP_MAX_DISTANCE

      if (isDoubleTap) {
        lastTapReference.current = null
        const overlappingNodeIds = getOverlappingNodeIds(
          document,
          getEditableElement,
          event.clientX,
          event.clientY,
        )
        const selectedNodeIndex = selectedNode
          ? overlappingNodeIds.indexOf(selectedNode.id)
          : -1
        const nodeIdToSelect =
          selectedNodeIndex >= 0
            ? overlappingNodeIds.at(selectedNodeIndex - 1)
            : undefined

        if (nodeIdToSelect) {
          onSelectNode(nodeIdToSelect, false)
        }
        event.preventDefault()

        return
      }

      const preferredNode =
        (selectedNode?.type === 'shape' ||
          selectedNode?.type === 'text' ||
          selectedNode?.type === 'raw') &&
        isPointerWithinNode(
          selectedNode,
          getEditableElement,
          event.clientX,
          event.clientY,
        )
          ? selectedNode
          : node

      const selectedGroup =
        selectedNode?.type === 'group' &&
        (isGroupDescendant(selectedNode, preferredNode.id) ||
          isPointerWithinNode(
            selectedNode,
            getEditableElement,
            event.clientX,
            event.clientY,
          ))
          ? selectedNode
          : null

      const selectedGroupToDrag = selectedGroup
      const isSelectedNode = selectedNodes.some(
        (selected) => selected.id === preferredNode.id,
      )

      if (
        (node.isLocked && node.type !== 'raw') ||
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
                (selected.type === 'shape' ||
                  selected.type === 'text' ||
                  selected.type === 'raw') &&
                (!selected.isLocked || selected.type === 'raw'),
            )
          : [preferredNode]

      if (dragTargets.length === 0) {
        return
      }

      if (!selectedGroupToDrag && !isSelectedNode) {
        onSelectNode(preferredNode.id, false)
      }

      const targets = dragTargets.flatMap((target) => {
        const element = getEditableElement(target.id)
        const dragTarget = element
          ? toDragTarget(target, element, event.clientX, event.clientY)
          : null

        return dragTarget ? [dragTarget] : []
      })

      if (targets.length !== dragTargets.length) {
        return
      }

      event.preventDefault()
      event.currentTarget.setPointerCapture(event.pointerId)
      dragStateReference.current = {
        hitNodeId: node.id,
        pointerId: event.pointerId,
        startClientPosition: { x: event.clientX, y: event.clientY },
        targets,
      }
      onDocumentChangeStart()
    },
    [
      activeTool,
      document,
      getEditableElement,
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

  const onRawPointerDown = useCallback(
    (node: EditorRawNode, event: ReactPointerEvent<SVGGElement>) => {
      startDrag(node, event)
    },
    [startDrag],
  )

  const onPointerMove = useCallback(
    (event: ReactPointerEvent<SVGGElement>) => {
      const dragState = dragStateReference.current
      if (!dragState || dragState.pointerId !== event.pointerId) {
        return
      }

      event.preventDefault()

      for (const target of dragState.targets) {
        const pointerPosition = getLocalPointerPosition(
          target.screenToLocalMatrix,
          event.clientX,
          event.clientY,
        )
        const offsetX = pointerPosition.x - target.startPointerPosition.x
        const offsetY = pointerPosition.y - target.startPointerPosition.y
        const x = roundCoordinate(target.startPosition.x + offsetX)
        const y = roundCoordinate(target.startPosition.y + offsetY)

        if (target.elementType === 'shape') {
          onShapePositionChange(target.nodeId, x, y)
        } else if (target.elementType === 'text') {
          onTextPositionChange(target.nodeId, x, y)
        } else if (target.elementType === 'raw') {
          onRawPositionChange(target.nodeId, x, y)
        } else {
          onGroupPositionChange(target.nodeId, x, y)
        }
      }
    },
    [
      onGroupPositionChange,
      onRawPositionChange,
      onShapePositionChange,
      onTextPositionChange,
    ],
  )

  const onPointerUp = useCallback(
    (event: ReactPointerEvent<SVGGElement>) => {
      const dragState = dragStateReference.current

      if (!dragState || dragState.pointerId !== event.pointerId) {
        return
      }

      if (event.currentTarget.hasPointerCapture(event.pointerId)) {
        event.currentTarget.releasePointerCapture(event.pointerId)
      }

      const horizontalOffset = event.clientX - dragState.startClientPosition.x
      const verticalOffset = event.clientY - dragState.startClientPosition.y
      const hasMoved =
        Math.hypot(horizontalOffset, verticalOffset) > TAP_MAX_DISTANCE

      dragStateReference.current = null

      if (!hasMoved && event.type !== 'pointercancel') {
        lastTapReference.current = {
          clientX: event.clientX,
          clientY: event.clientY,
          nodeId: dragState.hitNodeId,
          timestamp: event.timeStamp,
        }
      }

      onDocumentChangeEnd()
    },
    [onDocumentChangeEnd],
  )

  return {
    onPointerMove,
    onPointerUp,
    onRawPointerDown,
    onShapePointerDown,
    onTextPointerDown,
    svgReference,
  }
}
