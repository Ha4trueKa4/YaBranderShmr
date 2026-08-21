import { useEscAction, useLockBodyScroll } from '@shared/lib'
import { X } from 'lucide-react'
import type { PropsWithChildren, ReactNode } from 'react'

import { IconButton } from '../IconButton'
import { Portal } from '../Portal'
import styles from './BottomSheet.module.css'

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
  useLockBodyScroll(isOpen)
  useEscAction(onClose, isOpen)

  if (!isOpen) {
    return null
  }

  return (
    <Portal>
      <div className={styles.overlay}>
        <button className={styles.backdrop} onClick={onClose} type="button" />
        <section
          className={[styles.sheet, className].filter(Boolean).join(' ')}
        >
          <div className={styles.handle} />
          <header className={styles.header}>
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
