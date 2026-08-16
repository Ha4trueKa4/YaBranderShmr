import type { EditorNode } from '@entities/editorDocument'
import { observer } from 'mobx-react-lite'
import { useLayoutEffect, useMemo, useState } from 'react'

import {
  getSelectionBounds,
  type SelectionBounds,
} from './model/getSelectionBounds'
import styles from './SelectionOverlay.module.css'

const selectionPadding = 4

interface SelectionOverlayProps {
  getShapeElement: (nodeId: string) => SVGGElement | undefined
  selectedNode: EditorNode | null
}

export const SelectionOverlay = observer(function SelectionOverlay({
  getShapeElement,
  selectedNode,
}: SelectionOverlayProps) {
  const [bounds, setBounds] = useState<SelectionBounds | null>(null)
  const selectedShape = selectedNode?.type === 'shape' ? selectedNode : null
  const selectionTarget = useMemo(() => {
    if (!selectedShape) {
      return null
    }

    return {
      element: getShapeElement(selectedShape.id),
      transform: {
        rotation: selectedShape.transform.rotation,
        scaleX: selectedShape.transform.scaleX,
        scaleY: selectedShape.transform.scaleY,
        x: selectedShape.transform.x,
        y: selectedShape.transform.y,
      },
    }
  }, [
    getShapeElement,
    selectedShape,
    selectedShape?.transform.rotation,
    selectedShape?.transform.scaleX,
    selectedShape?.transform.scaleY,
    selectedShape?.transform.x,
    selectedShape?.transform.y,
  ])

  useLayoutEffect(() => {
    if (!selectionTarget) {
      setBounds(null)

      return
    }

    setBounds(getSelectionBounds(selectionTarget.element))
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
