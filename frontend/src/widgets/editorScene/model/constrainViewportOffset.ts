import type { ViewportOffset } from './viewportOffset'

interface Size {
  height: number
  width: number
}

const MIN_VISIBLE_ARTBOARD_SIZE = 48

function clamp(value: number, minimum: number, maximum: number) {
  return Math.min(Math.max(value, minimum), maximum)
}

export function constrainViewportOffset(
  offset: ViewportOffset,
  viewportSize: Size,
  artboardSize: Size,
  zoom: number,
): ViewportOffset {
  const artboardHeight = artboardSize.height * zoom
  const artboardWidth = artboardSize.width * zoom
  const visibleHeight = Math.min(MIN_VISIBLE_ARTBOARD_SIZE, artboardHeight)
  const visibleWidth = Math.min(MIN_VISIBLE_ARTBOARD_SIZE, artboardWidth)

  return {
    x: clamp(
      offset.x,
      -(viewportSize.width + artboardWidth) / 2 + visibleWidth,
      (viewportSize.width + artboardWidth) / 2 - visibleWidth,
    ),
    y: clamp(
      offset.y,
      -(viewportSize.height + artboardHeight) / 2 + visibleHeight,
      (viewportSize.height + artboardHeight) / 2 - visibleHeight,
    ),
  }
}
