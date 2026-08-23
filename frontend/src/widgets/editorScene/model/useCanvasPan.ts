import {
  type MutableRefObject,
  type PointerEvent as ReactPointerEvent,
  useCallback,
  useRef,
} from 'react'
import type { CanvasTool } from './canvasTool'
import type { ViewportOffset } from './viewportOffset'

interface UseCanvasPanParams {
  activeTool: CanvasTool
  constrainOffset: (offset: ViewportOffset) => ViewportOffset
  isNativeGestureActiveReference: MutableRefObject<boolean>
  offset: ViewportOffset
  onOffsetChange: (offset: ViewportOffset) => void
}

interface PanState {
  pointerId: number
  startOffset: ViewportOffset
  startPosition: { x: number; y: number }
  target: HTMLDivElement
}

export function useCanvasPan({
  activeTool,
  constrainOffset,
  isNativeGestureActiveReference,
  offset,
  onOffsetChange,
}: UseCanvasPanParams) {
  const panStateReference = useRef<PanState | null>(null)

  const onPointerDown = useCallback(
    (event: ReactPointerEvent<HTMLDivElement>) => {
      if (isNativeGestureActiveReference.current) {
        return
      }

      const canPanWithHandTool = activeTool === 'hand' && event.button === 0
      const canPanWithMiddleButton = event.button === 1

      if (!canPanWithHandTool && !canPanWithMiddleButton) {
        return
      }

      event.preventDefault()
      event.currentTarget.setPointerCapture(event.pointerId)
      panStateReference.current = {
        pointerId: event.pointerId,
        startOffset: offset,
        startPosition: { x: event.clientX, y: event.clientY },
        target: event.currentTarget,
      }
    },
    [activeTool, isNativeGestureActiveReference, offset],
  )

  const onPointerMove = useCallback(
    (event: ReactPointerEvent<HTMLDivElement>) => {
      const panState = panStateReference.current

      if (
        isNativeGestureActiveReference.current ||
        !panState ||
        panState.pointerId !== event.pointerId
      ) {
        return
      }

      event.preventDefault()
      onOffsetChange(
        constrainOffset({
          x: panState.startOffset.x + event.clientX - panState.startPosition.x,
          y: panState.startOffset.y + event.clientY - panState.startPosition.y,
        }),
      )
    },
    [constrainOffset, isNativeGestureActiveReference, onOffsetChange],
  )

  const stopPanning = useCallback(
    (event: ReactPointerEvent<HTMLDivElement>) => {
      if (panStateReference.current?.pointerId !== event.pointerId) {
        return
      }

      if (event.currentTarget.hasPointerCapture(event.pointerId)) {
        event.currentTarget.releasePointerCapture(event.pointerId)
      }

      panStateReference.current = null
    },
    [],
  )

  const cancelPan = useCallback(() => {
    const panState = panStateReference.current

    if (panState?.target.hasPointerCapture(panState.pointerId)) {
      panState.target.releasePointerCapture(panState.pointerId)
    }

    panStateReference.current = null
  }, [])

  return {
    cancelPan,
    onPointerCancel: stopPanning,
    onPointerDown,
    onPointerMove,
    onPointerUp: stopPanning,
  }
}
