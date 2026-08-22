import type { Transform } from '@entities/editorDocument'

type Point = NonNullable<Transform['rotationCenter']>

function round(value: number) {
  return Math.round(value * 10_000) / 10_000
}

/**
 * Масштабирует элемент вокруг его визуального центра, а не вокруг начала SVG.
 */
export function getCenteredScaleTransform(
  transform: Transform,
  center: Point,
  scale: number,
): Pick<Transform, 'rotationCenter' | 'scaleX' | 'scaleY' | 'x' | 'y'> {
  const scaleRatioX = scale / transform.scaleX
  const scaleRatioY = scale / transform.scaleY
  const rotationCenter = {
    x: center.x * scaleRatioX,
    y: center.y * scaleRatioY,
  }

  return {
    rotationCenter: {
      x: round(rotationCenter.x),
      y: round(rotationCenter.y),
    },
    scaleX: scale,
    scaleY: scale,
    x: round(transform.x + center.x - rotationCenter.x),
    y: round(transform.y + center.y - rotationCenter.y),
  }
}
