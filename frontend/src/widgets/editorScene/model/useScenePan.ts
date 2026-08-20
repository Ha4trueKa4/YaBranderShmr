import {
  type PointerEvent as ReactPointerEvent,
  useCallback,
  useRef,
} from 'react'

import type { CanvasTool } from './canvasTool'

interface ViewportOffset {
  x: number
  y: number
}

interface UseScenePanParams {
  activeTool: CanvasTool
  onViewportOffsetChange: (offset: ViewportOffset) => void
  viewportOffset: ViewportOffset
}

type PanState = {
  pointerId: number
  startPointerPosition: ViewportOffset
  startViewportOffset: ViewportOffset
}

export function useScenePan({
  activeTool,
  onViewportOffsetChange,
  viewportOffset,
}: UseScenePanParams) {
  const panStateReference = useRef<PanState | null>(null)

  const onPointerDown = useCallback(
    (event: ReactPointerEvent<SVGSVGElement>) => {
      if (activeTool !== 'hand' || event.button !== 0) {
        return
      }

      event.preventDefault()
      event.currentTarget.setPointerCapture(event.pointerId)
      panStateReference.current = {
        pointerId: event.pointerId,
        startPointerPosition: { x: event.clientX, y: event.clientY },
        startViewportOffset: viewportOffset,
      }
    },
    [activeTool, viewportOffset],
  )

  const onPointerMove = useCallback(
    (event: ReactPointerEvent<SVGSVGElement>) => {
      const panState = panStateReference.current

      if (!panState || panState.pointerId !== event.pointerId) {
        return
      }

      event.preventDefault()
      onViewportOffsetChange({
        x:
          panState.startViewportOffset.x +
          event.clientX -
          panState.startPointerPosition.x,
        y:
          panState.startViewportOffset.y +
          event.clientY -
          panState.startPointerPosition.y,
      })
    },
    [onViewportOffsetChange],
  )

  const onPointerUp = useCallback((event: ReactPointerEvent<SVGSVGElement>) => {
    if (panStateReference.current?.pointerId !== event.pointerId) {
      return
    }

    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId)
    }

    panStateReference.current = null
  }, [])

  return { onPointerDown, onPointerMove, onPointerUp }
}
