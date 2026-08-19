import { type RefObject, useEffect, useRef } from 'react'
import type { ViewportOffset } from './viewportOffset'

interface UseCanvasWheelParams {
  constrainOffset: (offset: ViewportOffset, zoom: number) => ViewportOffset
  offset: ViewportOffset
  onOffsetChange: (offset: ViewportOffset) => void
  onZoomChange: (zoom: number) => number
  viewportReference: RefObject<HTMLDivElement | null>
  zoom: number
}

const WHEEL_LINE_HEIGHT = 16
const ZOOM_SENSITIVITY = 0.003

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

function isMouseWheelZoom(event: WheelEvent) {
  const absoluteDeltaY = Math.abs(event.deltaY)

  return (
    event.ctrlKey ||
    event.metaKey ||
    event.deltaMode !== WheelEvent.DOM_DELTA_PIXEL ||
    (event.deltaX === 0 &&
      Number.isInteger(event.deltaY) &&
      absoluteDeltaY >= 100 &&
      (absoluteDeltaY % 100 === 0 || absoluteDeltaY % 120 === 0))
  )
}

export function useCanvasWheel({
  constrainOffset,
  offset,
  onOffsetChange,
  onZoomChange,
  viewportReference,
  zoom,
}: UseCanvasWheelParams) {
  const latestStateReference = useRef({
    constrainOffset,
    offset,
    onOffsetChange,
    onZoomChange,
    zoom,
  })
  const animationFrameReference = useRef<number | null>(null)

  useEffect(() => {
    latestStateReference.current = {
      offset,
      constrainOffset,
      onOffsetChange,
      onZoomChange,
      zoom,
    }
  }, [constrainOffset, offset, onOffsetChange, onZoomChange, zoom])

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

      if (!isMouseWheelZoom(event)) {
        scheduleOffsetChange(
          latestStateReference.current.constrainOffset(
            {
              x: offset.x - event.deltaX,
              y: offset.y - deltaY,
            },
            zoom,
          ),
        )

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

      const nextOffset = latestStateReference.current.constrainOffset(
        {
          x: cursorX - (cursorX - offset.x) * zoomRatio,
          y: cursorY - (cursorY - offset.y) * zoomRatio,
        },
        nextZoom,
      )

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
