import type { Project } from '@shared/api/types'
import { Button, IconButton, PopupMenu } from '@shared/ui'
import { MoreHorizontal } from 'lucide-react'
import type { KeyboardEvent, MouseEvent } from 'react'
import { useState } from 'react'

import { ProjectPreview } from '../projectPreview'
import styles from './OwnProjectCard.module.css'

export interface OwnProjectCardProps {
  onDeleteClick?: () => void
  onEditClick?: () => void
  onOpenClick?: () => void
  project: Project
}

export function OwnProjectCard({
  onDeleteClick,
  onEditClick,
  onOpenClick,
  project,
}: OwnProjectCardProps) {
  const [isMenuOpen, setMenuOpen] = useState(false)
  const isPrivate = !project.is_public
  const title = project.name ?? 'Untitled project'
  const updatedAt = getProjectStatus(project)
  const visibility = isPrivate ? 'Private' : 'Public'

  function handleMenuClick(event: MouseEvent<HTMLButtonElement>) {
    event.stopPropagation()
    setMenuOpen((isOpen) => !isOpen)
  }

  function handlePrimaryAction(event: MouseEvent<HTMLButtonElement>) {
    event.stopPropagation()
    onOpenClick?.()
  }

  function handleCardKeyDown(event: KeyboardEvent<HTMLElement>) {
    if (event.key === 'Enter' && event.currentTarget === event.target) {
      onOpenClick?.()
    }
  }

  return (
    <article
      className={styles.card}
      onClick={onOpenClick}
      onKeyDown={handleCardKeyDown}
    >
      <div className={styles.previewWrap}>
        <ProjectPreview imageUrl={project.url ?? null} title={title} />
        {!isPrivate && <span className={styles.visibility}>{visibility}</span>}
      </div>
      <div className={styles.content}>
        <div className={styles.titleRow}>
          <h3>{title}</h3>
          <IconButton
            onClick={handleMenuClick}
            onPointerDown={(event) => event.stopPropagation()}
            size="sm"
            title="Project actions"
          >
            <MoreHorizontal size={20} />
          </IconButton>
          {isMenuOpen && (
            <PopupMenu
              items={[
                { label: 'Edit project', onClick: () => onEditClick?.() },
                { label: 'Delete project', onClick: () => onDeleteClick?.() },
              ]}
              onClose={() => setMenuOpen(false)}
            />
          )}
        </div>
        <p>{isPrivate ? visibility : updatedAt}</p>
        <Button
          fullWidth
          onClick={handlePrimaryAction}
          size="sm"
          variant="secondary"
        >
          Open project
        </Button>
      </div>
    </article>
  )
}

function getProjectStatus(project: Project) {
  if (!project.is_public) {
    return 'Draft'
  }

  if (!project.created_at) {
    return 'Published'
  }

  return `Published ${new Intl.DateTimeFormat('en', {
    day: 'numeric',
    month: 'short',
  }).format(new Date(project.created_at))}`
}
