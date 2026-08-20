import { CircleAlert, Info, X } from 'lucide-react'
import { useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import styles from './Toast.module.css'
import type { Toast } from './toast.types'

const toastDuration = 5000

type ToastItemProps = {
  onDismiss: (id: Toast['id']) => void
  toast: Toast
}

export function ToastItem({ onDismiss, toast }: ToastItemProps) {
  const { t } = useTranslation()
  useEffect(() => {
    const timeoutId = window.setTimeout(
      () => onDismiss(toast.id),
      toastDuration,
    )

    return () => window.clearTimeout(timeoutId)
  }, [onDismiss, toast.id])

  const Icon = toast.variant === 'error' ? CircleAlert : Info

  return (
    <div className={`${styles.toast} ${styles[toast.variant]}`}>
      <Icon className={styles.icon} size={18} />
      <p className={styles.message}>{toast.message}</p>
      <button
        className={styles.dismissButton}
        onClick={() => onDismiss(toast.id)}
        title={t('Close notification')}
        type="button"
      >
        <X size={18} />
      </button>
    </div>
  )
}
