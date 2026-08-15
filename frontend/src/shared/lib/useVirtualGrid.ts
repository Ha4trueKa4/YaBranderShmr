import {
  type RefCallback,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react'

export interface VirtualGridBreakpoint {
  columnCount: number
  minWidth: number
}

export interface VirtualGridItem<T> {
  index: number
  item: T
}

export interface VirtualGridRow<T> {
  index: number
  items: VirtualGridItem<T>[]
  offsetTop: number
}

export interface VirtualGridLayout {
  columnCount: number
  containerWidth: number
}

export interface UseVirtualGridOptions<T> {
  breakpoints?: readonly VirtualGridBreakpoint[]
  defaultColumnCount?: number
  estimatedRowHeight: number | ((layout: VirtualGridLayout) => number)
  items: readonly T[]
  overscanRows?: number
  rowGap: number
  scrollRoot?: 'element' | 'window'
}

export interface UseVirtualGridResult<T> {
  columnCount: number
  scrollElementRef: RefCallback<HTMLElement>
  totalHeight: number
  virtualRows: VirtualGridRow<T>[]
}

interface Viewport {
  height: number
  scrollTop: number
  width: number
}

const INITIAL_VIEWPORT: Viewport = { height: 0, scrollTop: 0, width: 0 }

export function useVirtualGrid<T>({
  breakpoints = [],
  defaultColumnCount = 1,
  estimatedRowHeight,
  items,
  overscanRows = 2,
  rowGap,
  scrollRoot = 'element',
}: UseVirtualGridOptions<T>): UseVirtualGridResult<T> {
  const [scrollElement, setScrollElement] = useState<HTMLElement | null>(null)
  const [viewport, setViewport] = useState<Viewport>(INITIAL_VIEWPORT)

  const scrollElementRef = useCallback<RefCallback<HTMLElement>>((element) => {
    setScrollElement(element)
  }, [])

  useEffect(() => {
    if (!scrollElement) {
      return
    }

    let animationFrameId: number | undefined

    const updateViewport = () => {
      animationFrameId = undefined
      const isWindowScroll = scrollRoot === 'window'
      const elementTop = isWindowScroll
        ? window.scrollY + scrollElement.getBoundingClientRect().top
        : 0

      setViewport({
        height: isWindowScroll
          ? window.innerHeight
          : scrollElement.clientHeight,
        scrollTop: isWindowScroll
          ? Math.max(window.scrollY - elementTop, 0)
          : scrollElement.scrollTop,
        width: scrollElement.clientWidth,
      })
    }

    const scheduleViewportUpdate = () => {
      if (animationFrameId === undefined) {
        animationFrameId = requestAnimationFrame(updateViewport)
      }
    }

    const resizeObserver = new ResizeObserver(scheduleViewportUpdate)

    const scrollTarget = scrollRoot === 'window' ? window : scrollElement

    scrollTarget.addEventListener('scroll', scheduleViewportUpdate, {
      passive: true,
    })
    window.addEventListener('resize', scheduleViewportUpdate)
    resizeObserver.observe(scrollElement)
    updateViewport()

    return () => {
      if (animationFrameId !== undefined) {
        cancelAnimationFrame(animationFrameId)
      }

      scrollTarget.removeEventListener('scroll', scheduleViewportUpdate)
      window.removeEventListener('resize', scheduleViewportUpdate)
      resizeObserver.disconnect()
    }
  }, [scrollElement, scrollRoot])

  const columnCount = useMemo(
    () => getColumnCount(viewport.width, defaultColumnCount, breakpoints),
    [breakpoints, defaultColumnCount, viewport.width],
  )
  const rowHeight = Math.max(
    typeof estimatedRowHeight === 'function'
      ? estimatedRowHeight({
          columnCount,
          containerWidth: viewport.width,
        })
      : estimatedRowHeight,
    1,
  )
  const rowStride = rowHeight + Math.max(rowGap, 0)
  const rowCount = Math.ceil(items.length / columnCount)
  const totalHeight = Math.max(
    0,
    rowCount * rowHeight + Math.max(rowCount - 1, 0) * Math.max(rowGap, 0),
  )

  const virtualRows = useMemo(() => {
    const startRow = Math.max(
      0,
      Math.floor(viewport.scrollTop / rowStride) - overscanRows,
    )
    const endRow = Math.min(
      rowCount,
      Math.ceil((viewport.scrollTop + viewport.height) / rowStride) +
        overscanRows,
    )

    return Array.from(
      { length: Math.max(endRow - startRow, 0) },
      (_, offset) => {
        const index = startRow + offset
        const firstItemIndex = index * columnCount

        return {
          index,
          items: items
            .slice(firstItemIndex, firstItemIndex + columnCount)
            .map((item, itemOffset) => ({
              index: firstItemIndex + itemOffset,
              item,
            })),
          offsetTop: index * rowStride,
        }
      },
    )
  }, [columnCount, items, overscanRows, rowCount, rowStride, viewport])

  return { columnCount, scrollElementRef, totalHeight, virtualRows }
}

function getColumnCount(
  width: number,
  defaultColumnCount: number,
  breakpoints: readonly VirtualGridBreakpoint[],
) {
  const activeBreakpoint = breakpoints.reduce<
    VirtualGridBreakpoint | undefined
  >((currentBreakpoint, breakpoint) => {
    if (
      width >= breakpoint.minWidth &&
      (!currentBreakpoint || breakpoint.minWidth > currentBreakpoint.minWidth)
    ) {
      return breakpoint
    }

    return currentBreakpoint
  }, undefined)

  return Math.max(activeBreakpoint?.columnCount ?? defaultColumnCount, 1)
}
