import type { SelectionBounds } from './getSelectionBounds'

const selectionPadding = 4
const selectionStrokeWidth = 3

export function constrainSelectionBounds(
  bounds: SelectionBounds,
  canvasWidth: number,
  canvasHeight: number,
): SelectionBounds {
  const strokeInset = selectionStrokeWidth / 2
  const minimumX = strokeInset
  const minimumY = strokeInset
  const maximumX = canvasWidth - strokeInset
  const maximumY = canvasHeight - strokeInset
  const left = Math.min(
    Math.max(bounds.x - selectionPadding, minimumX),
    maximumX,
  )
  const top = Math.min(
    Math.max(bounds.y - selectionPadding, minimumY),
    maximumY,
  )
  const right = Math.min(
    Math.max(bounds.x + bounds.width + selectionPadding, minimumX),
    maximumX,
  )
  const bottom = Math.min(
    Math.max(bounds.y + bounds.height + selectionPadding, minimumY),
    maximumY,
  )

  return {
    height: Math.max(0, bottom - top),
    width: Math.max(0, right - left),
    x: left,
    y: top,
  }
}
