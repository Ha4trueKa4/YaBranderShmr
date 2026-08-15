import { authStore } from '@app/providers/auth'
import { getAvatarInitials } from '@shared/lib'
import { Avatar, Button } from '@shared/ui'
import chevronDown from '../assets/chevronDown.svg'
import styles from '../EditorToolbar.module.css'

interface ProjectActionsProps {
  onExport: () => void
  onOpenProfile: () => void
  onSaveVersion: () => void
}

export function ProjectActions({
  onExport,
  onOpenProfile,
  onSaveVersion,
}: ProjectActionsProps) {
  const session = authStore.getSession

  return (
    <div className={styles.projectActions}>
      <Button
        className={styles.saveVersion}
        onClick={onSaveVersion}
        variant="secondary"
      >
        Save version
      </Button>
      <Button onClick={onExport}>
        Export
        <img alt="" src={chevronDown} />
      </Button>
      {session && (
        <button className={styles.avatar} onClick={onOpenProfile} type="button">
          <Avatar initials={getAvatarInitials(session.user.nickname)} />
        </button>
      )}
    </div>
  )
}
