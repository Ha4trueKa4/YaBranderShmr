import { Button } from '@shared/ui'
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
      <button className={styles.avatar} onClick={onOpenProfile} type="button">
        OL
      </button>
    </div>
  )
}
