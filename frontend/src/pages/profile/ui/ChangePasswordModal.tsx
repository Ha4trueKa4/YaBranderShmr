import { useEscAction, useLockBodyScroll } from '@shared/lib'
import { Button, FormField, IconButton, Input, Portal } from '@shared/ui'
import { ArrowLeft, X } from 'lucide-react'
import { useId } from 'react'
import { useTranslation } from 'react-i18next'

import styles from './ChangePasswordModal.module.css'

interface ChangePasswordModalProps {
  isOpen: boolean
  onBack?: () => void
  onClose: () => void
}

export function ChangePasswordModal({
  isOpen,
  onBack,
  onClose,
}: ChangePasswordModalProps) {
  const { t } = useTranslation()
  const currentPasswordInputId = useId()
  const newPasswordInputId = useId()
  const confirmPasswordInputId = useId()

  useLockBodyScroll(isOpen)
  useEscAction(onClose, isOpen)

  if (!isOpen) {
    return null
  }

  return (
    <Portal>
      <div className={styles.overlay}>
        <button className={styles.backdrop} onClick={onClose} type="button" />
        <section className={styles.modal}>
          <header className={styles.header}>
            <div className={styles.titleGroup}>
              {onBack && (
                <IconButton
                  className={styles.backButton}
                  onClick={onBack}
                  size="sm"
                >
                  <ArrowLeft />
                </IconButton>
              )}
              <h2 className={styles.title}>{t('Change password')}</h2>
            </div>
            <IconButton
              className={styles.closeButton}
              onClick={onClose}
              size="sm"
            >
              <X />
            </IconButton>
          </header>
          <form
            className={styles.form}
            onSubmit={(event) => event.preventDefault()}
          >
            <FormField
              htmlFor={currentPasswordInputId}
              label={t('Current password')}
            >
              <Input
                autoComplete="current-password"
                autoFocus
                id={currentPasswordInputId}
                type="password"
              />
            </FormField>
            <FormField htmlFor={newPasswordInputId} label={t('New password')}>
              <Input
                autoComplete="new-password"
                id={newPasswordInputId}
                type="password"
              />
            </FormField>
            <FormField
              htmlFor={confirmPasswordInputId}
              label={t('Confirm new password')}
            >
              <Input
                autoComplete="new-password"
                id={confirmPasswordInputId}
                type="password"
              />
            </FormField>
            <div className={styles.actions}>
              <Button onClick={onClose} variant="secondary">
                {t('Cancel')}
              </Button>
              <Button type="submit">{t('Change password')}</Button>
            </div>
          </form>
        </section>
      </div>
    </Portal>
  )
}
