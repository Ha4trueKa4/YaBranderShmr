import { authStore } from '@app/providers/auth'
import { getAvatarInitials } from '@shared/lib'
import { Avatar, Button } from '@shared/ui'
import { Download, History, Save } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import styles from '../EditorToolbar.module.css'

interface ProjectActionsProps {
  canEdit: boolean
  isCreatingProjectCopy: boolean
  isSavingVersion: boolean
  onCreateCopy: () => void
  onExport: () => void
  onOpenProfile: () => void
  onOpenVersionHistory: () => void
  onSaveVersion: () => void
}

export function ProjectActions({
  canEdit,
  isCreatingProjectCopy,
  isSavingVersion,
  onCreateCopy,
  onExport,
  onOpenProfile,
  onOpenVersionHistory,
  onSaveVersion,
}: ProjectActionsProps) {
  const session = authStore.getSession
  const { t } = useTranslation()

  return (
    <div className={styles.projectActions}>
      {canEdit ? (
        <>
          <Button
            className={styles.saveVersion}
            loading={isSavingVersion}
            onClick={onSaveVersion}
            variant="secondary"
          >
            <Save className={styles.actionIcon} />
            {t('Save')}
          </Button>
          <Button onClick={onOpenVersionHistory} variant="secondary">
            <History className={styles.actionIcon} />
            {t('History')}
          </Button>
        </>
      ) : (
        <Button
          className={styles.saveVersion}
          loading={isCreatingProjectCopy}
          onClick={onCreateCopy}
          variant="secondary"
        >
          Create copy
        </Button>
      )}
      <Button onClick={onExport}>
        <Download className={styles.actionIcon} />
        Export
      </Button>
      {session && (
        <button className={styles.avatar} onClick={onOpenProfile} type="button">
          <Avatar initials={getAvatarInitials(session.user.nickname)} />
        </button>
      )}
    </div>
  )
}
