import type { Transform } from '@entities/editorDocument'

import type { SelectionBounds } from '../../selectionOverlay/model/getSelectionBounds'
import {
  getScaledTransform,
  type ScaleHandle,
  type ScalingOptions,
} from './getScaledTransform'

type Point = { x: number; y: number }

function isWestHandle(handle: ScaleHandle) {
  return handle === 'northWest' || handle === 'southWest' || handle === 'west'
}

function isNorthHandle(handle: ScaleHandle) {
  return handle === 'north' || handle === 'northEast' || handle === 'northWest'
}

function scalesHorizontal(handle: ScaleHandle) {
  return handle !== 'north' && handle !== 'south'
}

function scalesVertical(handle: ScaleHandle) {
  return handle !== 'east' && handle !== 'west'
}

function rotateVector(vector: Point, angle: number): Point {
  const angleInRadians = (angle * Math.PI) / 180
  const cosine = Math.cos(angleInRadians)
  const sine = Math.sin(angleInRadians)

  return {
    x: vector.x * cosine - vector.y * sine,
    y: vector.x * sine + vector.y * cosine,
  }
}

function round(value: number) {
  return Math.round(value * 10_000) / 10_000
}

/**
 * Масштабирует фигуру в её локальных осях и сохраняет неподвижным
 * противоположный угол уже повёрнутой рамки.
 */
export function getRotatedScaledTransform(
  bounds: SelectionBounds,
  handle: ScaleHandle,
  initialTransform: Transform,
  pointer: Point,
  options: ScalingOptions = {},
): Pick<Transform, 'rotationCenter' | 'scaleX' | 'scaleY' | 'x' | 'y'> {
  const scaledTransform = getScaledTransform(
    bounds,
    handle,
    initialTransform,
    pointer,
    options,
  )
  const rotationCenter = initialTransform.rotationCenter ?? {
    x: bounds.x + bounds.width / 2 - initialTransform.x,
    y: bounds.y + bounds.height / 2 - initialTransform.y,
  }
  const scaleRatioX = scaledTransform.scaleX / initialTransform.scaleX
  const scaleRatioY = scaledTransform.scaleY / initialTransform.scaleY
  const anchor = {
    x: options.scaleFromCenter
      ? rotationCenter.x
      : scalesHorizontal(handle)
        ? (isWestHandle(handle) ? bounds.x + bounds.width : bounds.x) -
          initialTransform.x
        : rotationCenter.x,
    y: options.scaleFromCenter
      ? rotationCenter.y
      : scalesVertical(handle)
        ? (isNorthHandle(handle) ? bounds.y + bounds.height : bounds.y) -
          initialTransform.y
        : rotationCenter.y,
  }
  const nextRotationCenter = {
    x: rotationCenter.x * scaleRatioX,
    y: rotationCenter.y * scaleRatioY,
  }
  const nextAnchor = {
    x: anchor.x * scaleRatioX,
    y: anchor.y * scaleRatioY,
  }
  const rotatedAnchor = rotateVector(
    {
      x: anchor.x - rotationCenter.x,
      y: anchor.y - rotationCenter.y,
    },
    initialTransform.rotation,
  )
  const nextRotatedAnchor = rotateVector(
    {
      x: nextAnchor.x - nextRotationCenter.x,
      y: nextAnchor.y - nextRotationCenter.y,
    },
    initialTransform.rotation,
  )
  const worldCenter = {
    x: initialTransform.x + rotationCenter.x,
    y: initialTransform.y + rotationCenter.y,
  }

  return {
    rotationCenter: {
      x: round(nextRotationCenter.x),
      y: round(nextRotationCenter.y),
    },
    scaleX: scaledTransform.scaleX,
    scaleY: scaledTransform.scaleY,
    x: round(
      worldCenter.x +
        rotatedAnchor.x -
        nextRotationCenter.x -
        nextRotatedAnchor.x,
    ),
    y: round(
      worldCenter.y +
        rotatedAnchor.y -
        nextRotationCenter.y -
        nextRotatedAnchor.y,
    ),
  }
}
