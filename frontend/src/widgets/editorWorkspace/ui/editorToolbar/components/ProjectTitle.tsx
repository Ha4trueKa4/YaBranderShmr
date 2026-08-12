import brandMark from '@shared/assets/brandmark.svg'
import { useState } from 'react'
import iconPencil from '../assets/iconPencil.svg'
import styles from '../EditorToolbar.module.css'

interface ProjectTitleProps {
  onSubmit: (projectName: string) => void
  title: string
}

export function ProjectTitle({ onSubmit, title }: ProjectTitleProps) {
  const [isEditing, setIsEditing] = useState(false)
  const [projectName, setProjectName] = useState(title)

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onSubmit(projectName)
    setIsEditing(false)
  }

  return (
    <div className={styles.projectTitle}>
      <img alt="" className={styles.brandMark} src={brandMark} />
      {isEditing ? (
        <form className={styles.projectNameForm} onSubmit={handleSubmit}>
          <input
            className={styles.projectNameInput}
            onChange={(event) => setProjectName(event.target.value)}
            value={projectName}
          />
        </form>
      ) : (
        <>
          <strong className={styles.projectName}>{projectName}</strong>
          <button
            className={styles.editProjectName}
            onClick={() => setIsEditing(true)}
            type="button"
          >
            <img alt="" src={iconPencil} />
          </button>
        </>
      )}
    </div>
  )
}
