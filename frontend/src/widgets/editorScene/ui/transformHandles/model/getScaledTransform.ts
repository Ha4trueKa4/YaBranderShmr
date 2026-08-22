import type { Transform } from '@entities/editorDocument'

import type { SelectionBounds } from '../../selectionOverlay/model/getSelectionBounds'

export type ScaleHandle =
  | 'east'
  | 'north'
  | 'northEast'
  | 'northWest'
  | 'south'
  | 'southEast'
  | 'southWest'
  | 'west'

type PointerPosition = { x: number; y: number }

const MAX_SCALE = 20
const MIN_SCALE = 0.05

function clamp(value: number, minimum: number, maximum: number) {
  return Math.min(Math.max(value, minimum), maximum)
}

function round(value: number) {
  return Math.round(value * 10_000) / 10_000
}

function scalesHorizontal(handle: ScaleHandle) {
  return handle !== 'north' && handle !== 'south'
}

function scalesVertical(handle: ScaleHandle) {
  return handle !== 'east' && handle !== 'west'
}

function isWestHandle(handle: ScaleHandle) {
  return handle === 'northWest' || handle === 'southWest' || handle === 'west'
}

function isNorthHandle(handle: ScaleHandle) {
  return handle === 'north' || handle === 'northEast' || handle === 'northWest'
}

function getNextScale(
  currentScale: number,
  initialSize: number,
  nextSize: number,
) {
  return clamp((currentScale * nextSize) / initialSize, MIN_SCALE, MAX_SCALE)
}

/**
 * Масштабирует фигуру вокруг противоположной стороны исходной рамки.
 * Пока используется только для неповёрнутых фигур: поворот добавит другую
 * систему координат, но интерфейс этой функции останется тем же.
 */
export function getScaledTransform(
  bounds: SelectionBounds,
  handle: ScaleHandle,
  initialTransform: Transform,
  pointer: PointerPosition,
): Pick<Transform, 'scaleX' | 'scaleY' | 'x' | 'y'> {
  const scalesX = scalesHorizontal(handle)
  const scalesY = scalesVertical(handle)
  const anchorX = isWestHandle(handle) ? bounds.x + bounds.width : bounds.x
  const anchorY = isNorthHandle(handle) ? bounds.y + bounds.height : bounds.y
  const nextWidth = isWestHandle(handle)
    ? anchorX - pointer.x
    : pointer.x - anchorX
  const nextHeight = isNorthHandle(handle)
    ? anchorY - pointer.y
    : pointer.y - anchorY
  const scaleX = scalesX
    ? getNextScale(initialTransform.scaleX, bounds.width, nextWidth)
    : initialTransform.scaleX
  const scaleY = scalesY
    ? getNextScale(initialTransform.scaleY, bounds.height, nextHeight)
    : initialTransform.scaleY

  return {
    scaleX: round(scaleX),
    scaleY: round(scaleY),
    x: round(
      scalesX
        ? anchorX -
            (scaleX / initialTransform.scaleX) * (anchorX - initialTransform.x)
        : initialTransform.x,
    ),
    y: round(
      scalesY
        ? anchorY -
            (scaleY / initialTransform.scaleY) * (anchorY - initialTransform.y)
        : initialTransform.y,
    ),
  }
}
