import { createPortal } from 'react-dom'
import styles from './Toast.module.css'
import { ToastItem } from './ToastItem'
import type { Toast } from './toast.types'

type ToastViewportProps = {
  onDismiss: (id: Toast['id']) => void
  toasts: Toast[]
}

export function ToastViewport({ onDismiss, toasts }: ToastViewportProps) {
  return createPortal(
    <div className={styles.viewport}>
      {toasts.map((toast) => (
        <ToastItem key={toast.id} onDismiss={onDismiss} toast={toast} />
      ))}
    </div>,
    document.body,
  )
}
