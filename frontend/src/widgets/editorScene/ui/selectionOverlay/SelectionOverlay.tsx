import type { EditorNode } from '@entities/editorDocument'
import { observer } from 'mobx-react-lite'
import { useLayoutEffect, useState } from 'react'

import { constrainSelectionBounds } from './model/constrainSelectionBounds'
import {
  getSelectionBounds,
  type SelectionBounds,
} from './model/getSelectionBounds'
import styles from './SelectionOverlay.module.css'

type SelectionBoundsByNode = {
  bounds: SelectionBounds
  nodeId: string
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

function hasSameSelection(
  firstSelection: SelectionBoundsByNode[],
  secondSelection: SelectionBoundsByNode[],
) {
  return (
    firstSelection.length === secondSelection.length &&
    firstSelection.every(
      (item, index) =>
        item.nodeId === secondSelection[index]?.nodeId &&
        hasSameBounds(item.bounds, secondSelection[index].bounds),
    )
  )
}

function getSelectedElements(node: EditorNode): EditorNode[] {
  if (node.type === 'group') {
    return node.children.flatMap(getSelectedElements)
  }

  return [node]
}

function getUniqueElements(nodes: EditorNode[]): EditorNode[] {
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
      const bounds = getSelectionBounds(getEditableElement(element.id))

      return bounds ? [{ bounds, nodeId: element.id }] : []
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

  return selectionBounds.map(({ bounds, nodeId }) => {
    const constrainedBounds = constrainSelectionBounds(
      bounds,
      canvasWidth,
      canvasHeight,
      canvasX,
      canvasY,
    )

    return (
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
