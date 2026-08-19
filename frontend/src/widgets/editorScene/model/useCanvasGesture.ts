import { type RefObject, useEffect, useRef } from 'react'
import type { ViewportOffset } from './viewportOffset'

interface GestureEvent extends Event {
  clientX: number
  clientY: number
  scale: number
}

interface UseCanvasGestureParams {
  offset: ViewportOffset
  onOffsetChange: (offset: ViewportOffset) => void
  onZoomChange: (zoom: number) => number
  viewportReference: RefObject<HTMLDivElement | null>
  zoom: number
}

interface GestureState {
  cursor: ViewportOffset
  offset: ViewportOffset
  zoom: number
}

const GESTURE_ZOOM_SENSITIVITY = 1.5

export function useCanvasGesture({
  offset,
  onOffsetChange,
  onZoomChange,
  viewportReference,
  zoom,
}: UseCanvasGestureParams) {
  const gestureStateReference = useRef<GestureState | null>(null)

  useEffect(() => {
    const viewport = viewportReference.current

    if (!viewport) {
      return
    }

    const viewportElement = viewport

    function handleGestureStart(event: Event) {
      const gestureEvent = event as GestureEvent
      const bounds = viewportElement.getBoundingClientRect()

      event.preventDefault()
      gestureStateReference.current = {
        cursor: {
          x: gestureEvent.clientX - bounds.left - bounds.width / 2,
          y: gestureEvent.clientY - bounds.top - bounds.height / 2,
        },
        offset,
        zoom,
      }
    }

    function handleGestureChange(event: Event) {
      const gestureState = gestureStateReference.current

      if (!gestureState) {
        return
      }

      event.preventDefault()
      const gestureScale = (event as GestureEvent).scale
      const nextZoom = onZoomChange(
        gestureState.zoom * gestureScale ** GESTURE_ZOOM_SENSITIVITY,
      )
      const zoomRatio = nextZoom / gestureState.zoom

      onOffsetChange({
        x:
          gestureState.cursor.x -
          (gestureState.cursor.x - gestureState.offset.x) * zoomRatio,
        y:
          gestureState.cursor.y -
          (gestureState.cursor.y - gestureState.offset.y) * zoomRatio,
      })
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
  }, [offset, onOffsetChange, onZoomChange, viewportReference, zoom])
}
