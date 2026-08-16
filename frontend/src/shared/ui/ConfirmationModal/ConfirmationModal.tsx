import { Button, type ButtonProps } from '../Button'
import { Portal } from '../Portal'
import styles from './ConfirmationModal.module.css'

export interface ConfirmationModalProps {
  cancelLabel?: string
  confirmLabel: string
  confirmVariant?: ButtonProps['variant']
  description: string
  isConfirming?: boolean
  isOpen: boolean
  onCancel: () => void
  onConfirm: () => void
  title: string
}

export function ConfirmationModal({
  cancelLabel = 'Cancel',
  confirmLabel,
  confirmVariant = 'danger',
  description,
  isConfirming = false,
  isOpen,
  onCancel,
  onConfirm,
  title,
}: ConfirmationModalProps) {
  if (!isOpen) {
    return null
  }

  return (
    <Portal>
      <div className={styles.overlay}>
        <button className={styles.backdrop} onClick={onCancel} type="button" />
        <section className={styles.modal}>
          <h2 className={styles.title}>{title}</h2>
          <p className={styles.description}>{description}</p>
          <div className={styles.actions}>
            <Button onClick={onCancel} variant="secondary">
              {cancelLabel}
            </Button>
            <Button
              loading={isConfirming}
              onClick={onConfirm}
              variant={confirmVariant}
            >
              {confirmLabel}
            </Button>
          </div>
        </section>
      </div>
    </Portal>
  )
}
