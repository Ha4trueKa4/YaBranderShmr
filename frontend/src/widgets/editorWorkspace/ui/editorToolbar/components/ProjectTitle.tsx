import { AppRoute } from '@app/providers/router/routes'
import brandMark from '@shared/assets/brandmark.svg'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import iconPencil from '../assets/iconPencil.svg'
import styles from '../EditorToolbar.module.css'
import { ProjectRenameModal } from './ProjectRenameModal'

interface ProjectTitleProps {
  onSubmit: (projectName: string) => Promise<void>
  title: string
}

export function ProjectTitle({ onSubmit, title }: ProjectTitleProps) {
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
      <Link className={styles.brandMark} to={AppRoute.EditorCreate}>
        <img alt="" src={brandMark} />
      </Link>
      <strong className={styles.projectName}>{title}</strong>
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
    </div>
  )
}
