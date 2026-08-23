import { useEscAction, useLockBodyScroll } from '@shared/lib'
import { X } from 'lucide-react'
import { Button, type ButtonProps } from '../Button'
import { IconButton } from '../IconButton'
import { Portal } from '../Portal'
import styles from './ConfirmationModal.module.css'

export interface ConfirmationModalProps {
  cancelLabel?: string
  cancelVariant?: ButtonProps['variant']
  confirmLabel: string
  confirmVariant?: ButtonProps['variant']
  description: string
  isCancelling?: boolean
  isConfirming?: boolean
  isOpen: boolean
  onCancel: () => void
  onConfirm: () => void
  onDismiss?: () => void
  title: string
}

export function ConfirmationModal({
  cancelLabel = 'Cancel',
  cancelVariant = 'secondary',
  confirmLabel,
  confirmVariant = 'danger',
  description,
  isCancelling = false,
  isConfirming = false,
  isOpen,
  onCancel,
  onConfirm,
  onDismiss = onCancel,
  title,
}: ConfirmationModalProps) {
  useLockBodyScroll(isOpen)
  useEscAction(onDismiss, isOpen)

  if (!isOpen) {
    return null
  }

  return (
    <Portal>
      <div className={styles.overlay}>
        <button className={styles.backdrop} onClick={onDismiss} type="button" />
        <section className={styles.modal}>
          <header className={styles.header}>
            <h2 className={styles.title}>{title}</h2>
            <IconButton
              className={styles.closeButton}
              onClick={onDismiss}
              size="sm"
            >
              <X />
            </IconButton>
          </header>
          <p className={styles.description}>{description}</p>
          <div className={styles.actions}>
            <Button
              disabled={isConfirming}
              loading={isCancelling}
              onClick={onCancel}
              variant={cancelVariant}
            >
              {cancelLabel}
            </Button>
            <Button
              disabled={isCancelling}
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
