import { useEscAction, useLockBodyScroll } from '@shared/lib'
import { X } from 'lucide-react'
import { Button, type ButtonProps } from '../Button'
import { IconButton } from '../IconButton'
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
  useLockBodyScroll(isOpen)
  useEscAction(onCancel, isOpen)

  if (!isOpen) {
    return null
  }

  return (
    <Portal>
      <div className={styles.overlay}>
        <button className={styles.backdrop} onClick={onCancel} type="button" />
        <section className={styles.modal}>
          <header className={styles.header}>
            <h2 className={styles.title}>{title}</h2>
            <IconButton
              className={styles.closeButton}
              onClick={onCancel}
              size="sm"
            >
              <X />
            </IconButton>
          </header>
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
