import type { PropsWithChildren, ReactNode } from 'react'

import { Portal } from '../Portal'
import styles from './BottomSheet.module.css'

export interface BottomSheetProps extends PropsWithChildren {
  actions?: ReactNode
  isOpen: boolean
  onClose: () => void
  title: string
}

export function BottomSheet({
  actions,
  children,
  isOpen,
  onClose,
  title,
}: BottomSheetProps) {
  if (!isOpen) {
    return null
  }

  return (
    <Portal>
      <div className={styles.overlay}>
        <button className={styles.backdrop} onClick={onClose} type="button" />
        <section className={styles.sheet}>
          <div className={styles.handle} />
          <header className={styles.header}>
            <h2>{title}</h2>
            {actions}
          </header>
          <div className={styles.content}>{children}</div>
        </section>
      </div>
    </Portal>
  )
}
