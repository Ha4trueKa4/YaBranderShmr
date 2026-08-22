import { useTranslation } from 'react-i18next'
import { ConfirmationModal } from '../ConfirmationModal'

export interface ConfirmUnsavedChangeModalProps {
  isOpen: boolean
  onCancel: () => void
  onConfirm: () => void
}

export function ConfirmUnsavedChangeModal({
  isOpen,
  onCancel,
  onConfirm,
}: ConfirmUnsavedChangeModalProps) {
  const { t } = useTranslation()

  return (
    <ConfirmationModal
      cancelLabel={t('Stay')}
      confirmLabel={t('Leave without saving')}
      description={t('Your unsaved changes will be lost.')}
      isOpen={isOpen}
      onCancel={onCancel}
      onConfirm={onConfirm}
      title={t('Leave without saving?')}
    />
  )
}
