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

export type ScalingOptions = {
  preserveAspectRatio?: boolean
  scaleFromCenter?: boolean
}

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

function isCornerHandle(handle: ScaleHandle) {
  return (
    handle === 'northEast' ||
    handle === 'northWest' ||
    handle === 'southEast' ||
    handle === 'southWest'
  )
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
  options: ScalingOptions = {},
): Pick<Transform, 'scaleX' | 'scaleY' | 'x' | 'y'> {
  const scalesX = scalesHorizontal(handle)
  const scalesY = scalesVertical(handle)
  const anchorX = isWestHandle(handle) ? bounds.x + bounds.width : bounds.x
  const anchorY = isNorthHandle(handle) ? bounds.y + bounds.height : bounds.y
  const centerX = bounds.x + bounds.width / 2
  const centerY = bounds.y + bounds.height / 2
  const nextWidth = options.scaleFromCenter
    ? Math.abs(pointer.x - centerX) * 2
    : isWestHandle(handle)
      ? anchorX - pointer.x
      : pointer.x - anchorX
  const nextHeight = options.scaleFromCenter
    ? Math.abs(pointer.y - centerY) * 2
    : isNorthHandle(handle)
      ? anchorY - pointer.y
      : pointer.y - anchorY
  let scaleX = scalesX
    ? getNextScale(initialTransform.scaleX, bounds.width, nextWidth)
    : initialTransform.scaleX
  let scaleY = scalesY
    ? getNextScale(initialTransform.scaleY, bounds.height, nextHeight)
    : initialTransform.scaleY

  if (options.preserveAspectRatio && isCornerHandle(handle)) {
    const horizontalRatio = scaleX / initialTransform.scaleX
    const verticalRatio = scaleY / initialTransform.scaleY
    const scaleRatio =
      Math.abs(horizontalRatio - 1) >= Math.abs(verticalRatio - 1)
        ? horizontalRatio
        : verticalRatio

    scaleX = round(initialTransform.scaleX * scaleRatio)
    scaleY = round(initialTransform.scaleY * scaleRatio)
  }

  return {
    scaleX: round(scaleX),
    scaleY: round(scaleY),
    x: round(
      options.scaleFromCenter && scalesX
        ? centerX -
            (scaleX / initialTransform.scaleX) * (centerX - initialTransform.x)
        : scalesX
          ? anchorX -
            (scaleX / initialTransform.scaleX) * (anchorX - initialTransform.x)
          : initialTransform.x,
    ),
    y: round(
      options.scaleFromCenter && scalesY
        ? centerY -
            (scaleY / initialTransform.scaleY) * (centerY - initialTransform.y)
        : scalesY
          ? anchorY -
            (scaleY / initialTransform.scaleY) * (anchorY - initialTransform.y)
          : initialTransform.y,
    ),
  }
}
