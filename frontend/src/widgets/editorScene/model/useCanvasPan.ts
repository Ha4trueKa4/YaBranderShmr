import {
  type PointerEvent as ReactPointerEvent,
  useCallback,
  useRef,
} from 'react'
import type { CanvasTool } from './canvasTool'
import type { ViewportOffset } from './viewportOffset'

interface UseCanvasPanParams {
  activeTool: CanvasTool
  offset: ViewportOffset
  onOffsetChange: (offset: ViewportOffset) => void
}

interface PanState {
  pointerId: number
  startOffset: ViewportOffset
  startPosition: { x: number; y: number }
}

export function useCanvasPan({
  activeTool,
  offset,
  onOffsetChange,
}: UseCanvasPanParams) {
  const panStateReference = useRef<PanState | null>(null)

  const onPointerDown = useCallback(
    (event: ReactPointerEvent<HTMLDivElement>) => {
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
      }
    },
    [activeTool, offset],
  )

  const onPointerMove = useCallback(
    (event: ReactPointerEvent<HTMLDivElement>) => {
      const panState = panStateReference.current

      if (!panState || panState.pointerId !== event.pointerId) {
        return
      }

      event.preventDefault()
      onOffsetChange({
        x: panState.startOffset.x + event.clientX - panState.startPosition.x,
        y: panState.startOffset.y + event.clientY - panState.startPosition.y,
      })
    },
    [onOffsetChange],
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

  return {
    onPointerCancel: stopPanning,
    onPointerDown,
    onPointerMove,
    onPointerUp: stopPanning,
  }
}
