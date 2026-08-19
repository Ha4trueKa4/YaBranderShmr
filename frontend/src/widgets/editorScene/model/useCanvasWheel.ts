import { type RefObject, useEffect, useRef } from 'react'
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
  const latestStateReference = useRef({
    offset,
    onOffsetChange,
    onZoomChange,
    zoom,
  })
  const animationFrameReference = useRef<number | null>(null)

  useEffect(() => {
    latestStateReference.current = {
      offset,
      onOffsetChange,
      onZoomChange,
      zoom,
    }
  }, [offset, onOffsetChange, onZoomChange, zoom])

  useEffect(() => {
    const viewport = viewportReference.current

    if (!viewport) {
      return
    }

    const viewportElement = viewport

    function scheduleOffsetChange(offset: ViewportOffset) {
      latestStateReference.current.offset = offset

      if (animationFrameReference.current !== null) {
        return
      }

      animationFrameReference.current = requestAnimationFrame(() => {
        animationFrameReference.current = null
        latestStateReference.current.onOffsetChange(
          latestStateReference.current.offset,
        )
      })
    }

    function handleWheel(event: WheelEvent) {
      event.preventDefault()

      const { offset, onOffsetChange, onZoomChange, zoom } =
        latestStateReference.current
      const deltaY = getWheelDeltaY(event, viewportElement)

      if (isTrackpadScroll(event)) {
        scheduleOffsetChange({
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

      const nextOffset = {
        x: cursorX - (cursorX - offset.x) * zoomRatio,
        y: cursorY - (cursorY - offset.y) * zoomRatio,
      }

      latestStateReference.current = {
        ...latestStateReference.current,
        offset: nextOffset,
        zoom: nextZoom,
      }
      onOffsetChange(nextOffset)
    }

    viewportElement.addEventListener('wheel', handleWheel, { passive: false })

    return () => {
      viewportElement.removeEventListener('wheel', handleWheel)

      if (animationFrameReference.current !== null) {
        cancelAnimationFrame(animationFrameReference.current)
      }
    }
  }, [viewportReference])
}
