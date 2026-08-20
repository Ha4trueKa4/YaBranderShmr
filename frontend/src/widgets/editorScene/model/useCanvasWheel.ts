import {
  type MutableRefObject,
  type RefObject,
  useEffect,
  useLayoutEffect,
  useRef,
} from 'react'
import type { ViewportOffset } from './viewportOffset'

interface UseCanvasWheelParams {
  constrainOffset: (offset: ViewportOffset, zoom: number) => ViewportOffset
  constrainZoom: (zoom: number) => number
  offset: ViewportOffset
  onOffsetChange: (offset: ViewportOffset) => void
  onZoomChange: (zoom: number) => number
  isNativeGestureActiveReference: MutableRefObject<boolean>
  viewportReference: RefObject<HTMLDivElement | null>
  zoom: number
}

const WHEEL_LINE_HEIGHT = 16
const ZOOM_SENSITIVITY = 0.003
const WHEEL_INTERACTION_TIMEOUT = 120

type WheelInteractionMode = 'pan' | 'zoom'

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
  isNativeGestureActiveReference,
  constrainOffset,
  constrainZoom,
  offset,
  onOffsetChange,
  onZoomChange,
  viewportReference,
  zoom,
}: UseCanvasWheelParams) {
  const latestStateReference = useRef({
    constrainOffset,
    constrainZoom,
    offset,
    onOffsetChange,
    onZoomChange,
    zoom,
  })
  const animationFrameReference = useRef<number | null>(null)
  const interactionModeReference = useRef<WheelInteractionMode | null>(null)
  const interactionTimeoutReference = useRef<number | null>(null)
  const pendingZoomReference = useRef<number | null>(null)

  useLayoutEffect(() => {
    latestStateReference.current = {
      offset,
      constrainOffset,
      constrainZoom,
      onOffsetChange,
      onZoomChange,
      zoom,
    }
  }, [
    constrainOffset,
    constrainZoom,
    offset,
    onOffsetChange,
    onZoomChange,
    zoom,
  ])

  useEffect(() => {
    const viewport = viewportReference.current

    if (!viewport) {
      return
    }

    const viewportElement = viewport

    function setInteractionMode(mode: WheelInteractionMode) {
      interactionModeReference.current = mode

      if (interactionTimeoutReference.current !== null) {
        clearTimeout(interactionTimeoutReference.current)
      }

      interactionTimeoutReference.current = window.setTimeout(() => {
        interactionModeReference.current = null
        interactionTimeoutReference.current = null
      }, WHEEL_INTERACTION_TIMEOUT)
    }

    function scheduleViewportChange(offset: ViewportOffset, nextZoom?: number) {
      latestStateReference.current.offset = offset

      if (nextZoom !== undefined) {
        latestStateReference.current.zoom = nextZoom
        pendingZoomReference.current = nextZoom
      }

      if (animationFrameReference.current !== null) {
        return
      }

      animationFrameReference.current = requestAnimationFrame(() => {
        animationFrameReference.current = null
        const pendingZoom = pendingZoomReference.current

        pendingZoomReference.current = null

        if (pendingZoom !== null) {
          latestStateReference.current.onZoomChange(pendingZoom)
        }

        latestStateReference.current.onOffsetChange(
          latestStateReference.current.offset,
        )
      })
    }

    function handleWheel(event: WheelEvent) {
      event.preventDefault()

      if (isNativeGestureActiveReference.current) {
        return
      }

      const { constrainZoom, offset, zoom } = latestStateReference.current
      const deltaY = getWheelDeltaY(event, viewportElement)

      const isZoomInteraction =
        isMouseWheelZoom(event) || interactionModeReference.current === 'zoom'

      setInteractionMode(isZoomInteraction ? 'zoom' : 'pan')

      if (!isZoomInteraction) {
        scheduleViewportChange(
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

      const nextZoom = constrainZoom(
        zoom * Math.exp(-deltaY * ZOOM_SENSITIVITY),
      )

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

      scheduleViewportChange(nextOffset, nextZoom)
    }

    viewportElement.addEventListener('wheel', handleWheel, { passive: false })

    return () => {
      viewportElement.removeEventListener('wheel', handleWheel)

      if (animationFrameReference.current !== null) {
        cancelAnimationFrame(animationFrameReference.current)
      }

      pendingZoomReference.current = null

      if (interactionTimeoutReference.current !== null) {
        clearTimeout(interactionTimeoutReference.current)
      }
    }
  }, [isNativeGestureActiveReference, viewportReference])
}
