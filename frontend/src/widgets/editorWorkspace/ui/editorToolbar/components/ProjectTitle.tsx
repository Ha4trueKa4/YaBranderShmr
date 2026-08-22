import { useState } from 'react'
import iconPencil from '../assets/iconPencil.svg'
import styles from '../EditorToolbar.module.css'
import { ProjectRenameModal } from './ProjectRenameModal'

interface ProjectTitleProps {
  canEdit: boolean
  onSubmit: (projectName: string) => Promise<void>
  title: string
}

export function ProjectTitle({ canEdit, onSubmit, title }: ProjectTitleProps) {
  const [isRenameModalOpen, setRenameModalOpen] = useState(false)

  async function handleSubmit(projectName: string) {
    try {
      await onSubmit(projectName)
      setRenameModalOpen(false)
    } catch {
      // Сообщение об ошибке уже показывает родительский компонент.
    }
  }

  return (
    <div className={styles.projectTitle}>
      <strong className={styles.projectName}>{title}</strong>
      {canEdit && (
        <>
          <button
            className={styles.editProjectName}
            onClick={() => setRenameModalOpen(true)}
            type="button"
          >
            <img alt="" src={iconPencil} />
          </button>
          <ProjectRenameModal
            isOpen={isRenameModalOpen}
            onClose={() => setRenameModalOpen(false)}
            onSubmit={handleSubmit}
            projectName={title}
          />
        </>
      )}
    </div>
  )
}
