import type { EditorNode } from '@entities/editorDocument'
import { observer } from 'mobx-react-lite'
import { useLayoutEffect, useState } from 'react'

import {
  getSelectionBounds,
  type SelectionBounds,
} from './model/getSelectionBounds'
import styles from './SelectionOverlay.module.css'

const selectionPadding = 4

function hasSameBounds(
  firstBounds: SelectionBounds | null,
  secondBounds: SelectionBounds | null,
) {
  return (
    firstBounds?.height === secondBounds?.height &&
    firstBounds?.width === secondBounds?.width &&
    firstBounds?.x === secondBounds?.x &&
    firstBounds?.y === secondBounds?.y
  )
}

interface SelectionOverlayProps {
  getEditableElement: (nodeId: string) => SVGGElement | undefined
  selectedNode: EditorNode | null
}

export const SelectionOverlay = observer(function SelectionOverlay({
  getEditableElement,
  selectedNode,
}: SelectionOverlayProps) {
  const [bounds, setBounds] = useState<SelectionBounds | null>(null)
  const selectedElement =
    selectedNode?.type === 'shape' || selectedNode?.type === 'text'
      ? selectedNode
      : null
  const selectionTarget = selectedElement
    ? {
        element: getEditableElement(selectedElement.id),
        position:
          selectedElement.type === 'text'
            ? { x: selectedElement.x, y: selectedElement.y }
            : undefined,
        transform: {
          rotation: selectedElement.transform.rotation,
          scaleX: selectedElement.transform.scaleX,
          scaleY: selectedElement.transform.scaleY,
          x: selectedElement.transform.x,
          y: selectedElement.transform.y,
        },
      }
    : null

  useLayoutEffect(() => {
    if (!selectionTarget) {
      setBounds((currentBounds) =>
        hasSameBounds(currentBounds, null) ? currentBounds : null,
      )

      return
    }

    const nextBounds = getSelectionBounds(selectionTarget.element)

    setBounds((currentBounds) =>
      hasSameBounds(currentBounds, nextBounds) ? currentBounds : nextBounds,
    )
  }, [selectionTarget])

  if (!bounds) {
    return null
  }

  return (
    <rect
      className={styles.overlay}
      height={bounds.height + selectionPadding * 2}
      width={bounds.width + selectionPadding * 2}
      x={bounds.x - selectionPadding}
      y={bounds.y - selectionPadding}
    />
  )
})
