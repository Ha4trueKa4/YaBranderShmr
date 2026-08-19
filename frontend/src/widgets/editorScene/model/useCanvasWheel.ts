import { type RefObject, useEffect } from 'react'
import type { ViewportOffset } from './viewportOffset'

interface UseCanvasWheelParams {
  offset: ViewportOffset
  onOffsetChange: (offset: ViewportOffset) => void
  onZoomChange: (zoom: number) => number
  viewportReference: RefObject<HTMLDivElement | null>
  zoom: number
}

const WHEEL_LINE_HEIGHT = 16
const ZOOM_SENSITIVITY = 0.003
const TRACKPAD_DELTA_THRESHOLD = 50

function getWheelDeltaY(event: WheelEvent, viewport: HTMLDivElement) {
  if (event.deltaMode === WheelEvent.DOM_DELTA_LINE) {
    return event.deltaY * WHEEL_LINE_HEIGHT
  }

  if (event.deltaMode === WheelEvent.DOM_DELTA_PAGE) {
    const { height } = viewport.getBoundingClientRect()

    return event.deltaY * height
  }

  return event.deltaY
}

function isTrackpadScroll(event: WheelEvent) {
  return (
    !event.ctrlKey &&
    !event.metaKey &&
    event.deltaMode === WheelEvent.DOM_DELTA_PIXEL &&
    (event.deltaX !== 0 || Math.abs(event.deltaY) < TRACKPAD_DELTA_THRESHOLD)
  )
}

export function useCanvasWheel({
  offset,
  onOffsetChange,
  onZoomChange,
  viewportReference,
  zoom,
}: UseCanvasWheelParams) {
  useEffect(() => {
    const viewport = viewportReference.current

    if (!viewport) {
      return
    }

    const viewportElement = viewport

    function handleWheel(event: WheelEvent) {
      event.preventDefault()

      const deltaY = getWheelDeltaY(event, viewportElement)

      if (isTrackpadScroll(event)) {
        onOffsetChange({
          x: offset.x - event.deltaX,
          y: offset.y - deltaY,
        })

        return
      }

      const nextZoom = onZoomChange(zoom * Math.exp(-deltaY * ZOOM_SENSITIVITY))

      if (nextZoom === zoom) {
        return
      }

      const bounds = viewportElement.getBoundingClientRect()
      const cursorX = event.clientX - bounds.left - bounds.width / 2
      const cursorY = event.clientY - bounds.top - bounds.height / 2
      const zoomRatio = nextZoom / zoom

      onOffsetChange({
        x: cursorX - (cursorX - offset.x) * zoomRatio,
        y: cursorY - (cursorY - offset.y) * zoomRatio,
      })
    }

    viewportElement.addEventListener('wheel', handleWheel, { passive: false })

    return () => viewportElement.removeEventListener('wheel', handleWheel)
  }, [offset, onOffsetChange, onZoomChange, viewportReference, zoom])
}
