import type { EditorElement, EditorNode } from '@entities/editorDocument'
import { observer } from 'mobx-react-lite'
import { useLayoutEffect, useState } from 'react'

import { constrainSelectionBounds } from './model/constrainSelectionBounds'
import {
  getSelectionBounds,
  getSelectionOutline,
  type SelectionBounds,
  type SelectionPoint,
} from './model/getSelectionBounds'
import styles from './SelectionOverlay.module.css'

type SelectionBoundsByNode = {
  bounds: SelectionBounds
  nodeId: string
  outline: SelectionPoint[] | null
}

function hasSameBounds(
  firstBounds: SelectionBounds,
  secondBounds: SelectionBounds,
) {
  return (
    firstBounds.height === secondBounds.height &&
    firstBounds.width === secondBounds.width &&
    firstBounds.x === secondBounds.x &&
    firstBounds.y === secondBounds.y
  )
}

function hasSameOutline(
  firstOutline: SelectionPoint[] | null,
  secondOutline: SelectionPoint[] | null,
) {
  if (!firstOutline || !secondOutline) {
    return firstOutline === secondOutline
  }

  return (
    firstOutline.length === secondOutline.length &&
    firstOutline.every(
      (point, index) =>
        point.x === secondOutline[index]?.x &&
        point.y === secondOutline[index]?.y,
    )
  )
}

function hasSameSelection(
  firstSelection: SelectionBoundsByNode[],
  secondSelection: SelectionBoundsByNode[],
) {
  return (
    firstSelection.length === secondSelection.length &&
    firstSelection.every(
      (item, index) =>
        item.nodeId === secondSelection[index]?.nodeId &&
        hasSameBounds(item.bounds, secondSelection[index].bounds) &&
        hasSameOutline(item.outline, secondSelection[index].outline),
    )
  )
}

function getSelectedElements(node: EditorNode): EditorElement[] {
  if (node.type === 'raw') {
    return []
  }

  if (node.type === 'group') {
    return node.children.flatMap(getSelectedElements)
  }

  return [node]
}

function getUniqueElements(nodes: EditorNode[]): EditorElement[] {
  for (const node of nodes) {
    getNodeRenderSignature(node)
  }

  const elements = nodes.flatMap(getSelectedElements)

  return [...new Map(elements.map((element) => [element.id, element])).values()]
}

function getNodeRenderSignature(node: EditorNode): string {
  const transform = [
    node.transform.x,
    node.transform.y,
    node.transform.rotation,
    node.transform.scaleX,
    node.transform.scaleY,
    node.transform.rotationCenter?.x ?? '',
    node.transform.rotationCenter?.y ?? '',
  ].join(',')

  if (node.type === 'group') {
    return `${node.id}:${transform}[${node.children.map(getNodeRenderSignature).join('|')}]`
  }

  if (node.type === 'text') {
    return `${node.id}:${transform}:${node.x},${node.y}:${node.text}:${node.style.fontFamily},${node.style.fontSize},${node.style.fontStyle ?? ''},${node.style.fontWeight ?? ''},${node.style.letterSpacing ?? ''}`
  }

  if (node.type === 'shape') {
    return `${node.id}:${transform}:${JSON.stringify(node.geometry)}:${node.style.strokeWidth}`
  }

  return node.id
}

interface SelectionOverlayProps {
  canvasHeight: number
  canvasWidth: number
  canvasX: number
  canvasY: number
  getEditableElement: (nodeId: string) => SVGGElement | undefined
  selectedNodes: EditorNode[]
}

export const SelectionOverlay = observer(function SelectionOverlay({
  canvasHeight,
  canvasWidth,
  canvasX,
  canvasY,
  getEditableElement,
  selectedNodes,
}: SelectionOverlayProps) {
  const [selectionBounds, setSelectionBounds] = useState<
    SelectionBoundsByNode[]
  >([])
  const selectedElements = getUniqueElements(selectedNodes)

  useLayoutEffect(() => {
    const nextSelection = selectedElements.flatMap((element) => {
      const editableElement = getEditableElement(element.id)
      const bounds = getSelectionBounds(editableElement)

      return bounds
        ? [
            {
              bounds,
              nodeId: element.id,
              outline:
                element.transform.rotation === 0
                  ? null
                  : getSelectionOutline(editableElement, 4),
            },
          ]
        : []
    })

    setSelectionBounds((currentSelection) =>
      hasSameSelection(currentSelection, nextSelection)
        ? currentSelection
        : nextSelection,
    )
  }, [getEditableElement, selectedElements])

  if (selectionBounds.length === 0) {
    return null
  }

  return selectionBounds.map(({ bounds, nodeId, outline }) => {
    const constrainedBounds = constrainSelectionBounds(
      bounds,
      canvasWidth,
      canvasHeight,
      canvasX,
      canvasY,
    )

    return outline ? (
      <polygon
        className={styles.overlay}
        key={nodeId}
        points={outline.map((point) => `${point.x},${point.y}`).join(' ')}
      />
    ) : (
      <rect
        className={styles.overlay}
        height={constrainedBounds.height}
        key={nodeId}
        width={constrainedBounds.width}
        x={constrainedBounds.x}
        y={constrainedBounds.y}
      />
    )
  })
})
