import { useEscAction, useLockBodyScroll } from '@shared/lib'
import { X } from 'lucide-react'
import {
  type PropsWithChildren,
  type ReactNode,
  type PointerEvent as ReactPointerEvent,
  useRef,
  useState,
} from 'react'

import { IconButton } from '../IconButton'
import { Portal } from '../Portal'
import styles from './BottomSheet.module.css'

const dismissDistance = 96

interface DragState {
  pointerId: number
  startY: number
}

export interface BottomSheetProps extends PropsWithChildren {
  actions?: ReactNode
  className?: string
  contentClassName?: string
  isOpen: boolean
  onClose: () => void
  title: string
}

export function BottomSheet({
  actions,
  children,
  className,
  contentClassName,
  isOpen,
  onClose,
  title,
}: BottomSheetProps) {
  const dragStateRef = useRef<DragState | null>(null)
  const [dragOffset, setDragOffset] = useState(0)
  const [isDragging, setIsDragging] = useState(false)

  useLockBodyScroll(isOpen)
  useEscAction(onClose, isOpen)

  if (!isOpen) {
    return null
  }

  function handleDragStart(event: ReactPointerEvent<HTMLElement>) {
    if (
      event.button !== 0 ||
      (event.target instanceof Element && event.target.closest('button'))
    ) {
      return
    }

    dragStateRef.current = {
      pointerId: event.pointerId,
      startY: event.clientY,
    }
    setIsDragging(true)
    event.currentTarget.setPointerCapture(event.pointerId)
  }

  function handleDragMove(event: ReactPointerEvent<HTMLElement>) {
    const dragState = dragStateRef.current

    if (!dragState || dragState.pointerId !== event.pointerId) {
      return
    }

    setDragOffset(Math.max(0, event.clientY - dragState.startY))
  }

  function resetDrag(event: ReactPointerEvent<HTMLElement>) {
    const dragState = dragStateRef.current

    if (!dragState || dragState.pointerId !== event.pointerId) {
      return null
    }

    dragStateRef.current = null
    setDragOffset(0)
    setIsDragging(false)

    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId)
    }

    return dragState
  }

  function handleDragEnd(event: ReactPointerEvent<HTMLElement>) {
    const dragState = resetDrag(event)

    if (!dragState) {
      return
    }

    if (event.clientY - dragState.startY >= dismissDistance) {
      onClose()
    }
  }

  function handleDragCancel(event: ReactPointerEvent<HTMLElement>) {
    resetDrag(event)
  }

  return (
    <Portal>
      <div className={styles.overlay}>
        <button className={styles.backdrop} onClick={onClose} type="button" />
        <section
          className={[
            styles.sheet,
            isDragging && styles.sheetDragging,
            className,
          ]
            .filter(Boolean)
            .join(' ')}
          style={{ transform: `translateY(${dragOffset}px)` }}
        >
          <div
            className={styles.handle}
            onPointerCancel={handleDragCancel}
            onPointerDown={handleDragStart}
            onPointerMove={handleDragMove}
            onPointerUp={handleDragEnd}
          />
          <header
            className={styles.header}
            onPointerCancel={handleDragCancel}
            onPointerDown={handleDragStart}
            onPointerMove={handleDragMove}
            onPointerUp={handleDragEnd}
          >
            <h2>{title}</h2>
            <div className={styles.headerActions}>
              {actions}
              <IconButton onClick={onClose} size="sm">
                <X />
              </IconButton>
            </div>
          </header>
          <div
            className={[styles.content, contentClassName]
              .filter(Boolean)
              .join(' ')}
          >
            {children}
          </div>
        </section>
      </div>
    </Portal>
  )
}
