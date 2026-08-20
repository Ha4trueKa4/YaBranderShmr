import type { EditorElement, EditorNode } from '@entities/editorDocument'
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
  const elements = nodes.flatMap(getSelectedElements)

  return [...new Map(elements.map((element) => [element.id, element])).values()]
}

interface SelectionOverlayProps {
  canvasHeight: number
  canvasWidth: number
  getEditableElement: (nodeId: string) => SVGGElement | undefined
  selectedNodes: EditorNode[]
}

export const SelectionOverlay = observer(function SelectionOverlay({
  canvasHeight,
  canvasWidth,
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
