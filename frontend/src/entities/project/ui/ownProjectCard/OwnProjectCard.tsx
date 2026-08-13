import type { Project } from '@shared/api/types'
import { Button, IconButton } from '@shared/ui'
import { MoreHorizontal } from 'lucide-react'

import { ProjectPreview } from '../projectPreview'
import styles from './OwnProjectCard.module.css'

export interface OwnProjectCardProps {
  onMenuClick?: () => void
  onOpenClick?: () => void
  project: Project
}

export function OwnProjectCard({
  onMenuClick,
  onOpenClick,
  project,
}: OwnProjectCardProps) {
  const isPrivate = !project.is_public
  const actionLabel = isPrivate ? 'Manage private project' : 'Open project'
  const title = project.name ?? 'Untitled project'
  const updatedAt = getProjectStatus(project)
  const visibility = isPrivate ? 'Private' : 'Public'

  return (
    <article className={styles.card}>
      <div className={styles.previewWrap}>
        <ProjectPreview title={title} />
        {isPrivate && <span className={styles.visibility}>{visibility}</span>}
      </div>
      <div className={styles.content}>
        <div className={styles.titleRow}>
          <h3>{title}</h3>
          <IconButton onClick={onMenuClick} size="sm" title="Project actions">
            <MoreHorizontal size={20} />
          </IconButton>
        </div>
        <p>
          {visibility} · {updatedAt}
        </p>
        <Button
          className={isPrivate ? styles.privateAction : undefined}
          fullWidth
          onClick={onOpenClick}
          size="sm"
          variant="secondary"
        >
          {actionLabel}
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
