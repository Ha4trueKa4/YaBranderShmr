import { type RefObject, useEffect, useRef } from 'react'
import type { ViewportOffset } from './viewportOffset'

interface GestureEvent extends Event {
  clientX: number
  clientY: number
  scale: number
}

interface UseCanvasGestureParams {
  constrainOffset: (offset: ViewportOffset, zoom: number) => ViewportOffset
  offset: ViewportOffset
  onOffsetChange: (offset: ViewportOffset) => void
  onZoomChange: (zoom: number) => number
  viewportReference: RefObject<HTMLDivElement | null>
  zoom: number
}

interface GestureState {
  cursor: ViewportOffset
  isZooming: boolean
  offset: ViewportOffset
  zoom: number
}

const GESTURE_ZOOM_SENSITIVITY = 1.5
const PINCH_SCALE_THRESHOLD = 0.05

export function useCanvasGesture({
  constrainOffset,
  offset,
  onOffsetChange,
  onZoomChange,
  viewportReference,
  zoom,
}: UseCanvasGestureParams) {
  const gestureStateReference = useRef<GestureState | null>(null)
  const latestStateReference = useRef({
    constrainOffset,
    offset,
    onOffsetChange,
    onZoomChange,
    zoom,
  })

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

    function handleGestureStart(event: Event) {
      const gestureEvent = event as GestureEvent
      const bounds = viewportElement.getBoundingClientRect()
      const { offset, zoom } = latestStateReference.current

      gestureStateReference.current = {
        cursor: {
          x: gestureEvent.clientX - bounds.left - bounds.width / 2,
          y: gestureEvent.clientY - bounds.top - bounds.height / 2,
        },
        isZooming: false,
        offset,
        zoom,
      }
    }

    function handleGestureChange(event: Event) {
      const gestureState = gestureStateReference.current

      if (!gestureState) {
        return
      }

      const { onOffsetChange, onZoomChange } = latestStateReference.current
      const gestureScale = (event as GestureEvent).scale

      if (
        !gestureState.isZooming &&
        Math.abs(gestureScale - 1) < PINCH_SCALE_THRESHOLD
      ) {
        return
      }

      gestureState.isZooming = true
      event.preventDefault()
      const nextZoom = onZoomChange(
        gestureState.zoom * gestureScale ** GESTURE_ZOOM_SENSITIVITY,
      )
      const zoomRatio = nextZoom / gestureState.zoom

      const nextOffset = latestStateReference.current.constrainOffset(
        {
          x:
            gestureState.cursor.x -
            (gestureState.cursor.x - gestureState.offset.x) * zoomRatio,
          y:
            gestureState.cursor.y -
            (gestureState.cursor.y - gestureState.offset.y) * zoomRatio,
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

    function handleGestureEnd() {
      gestureStateReference.current = null
    }

    viewportElement.addEventListener('gesturestart', handleGestureStart, {
      passive: false,
    })
    viewportElement.addEventListener('gesturechange', handleGestureChange, {
      passive: false,
    })
    viewportElement.addEventListener('gestureend', handleGestureEnd)

    return () => {
      viewportElement.removeEventListener('gesturestart', handleGestureStart)
      viewportElement.removeEventListener('gesturechange', handleGestureChange)
      viewportElement.removeEventListener('gestureend', handleGestureEnd)
    }
  }, [viewportReference])
}
