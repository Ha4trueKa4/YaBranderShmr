import { useTranslation } from 'react-i18next'
import { ConfirmationModal } from '../ConfirmationModal'

export interface ConfirmUnsavedChangeModalProps {
  isOpen: boolean
  isSaving: boolean
  onDismiss: () => void
  onLeave: () => void
  onSave: () => void
}

export function ConfirmUnsavedChangeModal({
  isOpen,
  isSaving,
  onDismiss,
  onLeave,
  onSave,
}: ConfirmUnsavedChangeModalProps) {
  const { t } = useTranslation()

  return (
    <ConfirmationModal
      cancelLabel={t('Save')}
      cancelVariant="primary"
      confirmLabel={t('Leave')}
      description={t('Your unsaved changes will be lost.')}
      isCancelling={isSaving}
      isOpen={isOpen}
      onCancel={onSave}
      onConfirm={onLeave}
      onDismiss={onDismiss}
      title={t('Leave without saving?')}
    />
  )
}
