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
  return (
    <ConfirmationModal
      cancelLabel="Stay"
      confirmLabel="Leave without saving"
      description="Your unsaved changes will be lost."
      isOpen={isOpen}
      onCancel={onCancel}
      onConfirm={onConfirm}
      title="Leave without saving?"
    />
  )
}
