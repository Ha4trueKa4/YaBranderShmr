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
      <button
        className={styles.saveVersion}
        onClick={onSaveVersion}
        type="button"
      >
        Save version
      </button>
      <button className={styles.export} onClick={onExport} type="button">
        Export
        <img alt="" src={chevronDown} />
      </button>
      <button className={styles.avatar} onClick={onOpenProfile} type="button">
        OL
      </button>
    </div>
  )
}
