import { Button, IconButton } from '@shared/ui'
import { MoreHorizontal } from 'lucide-react'

import { ProjectPreview } from '../projectPreview'
import styles from './OwnProjectCard.module.css'

export interface OwnProjectCardProps {
  isPrivate: boolean
  onMenuClick?: () => void
  onOpenClick?: () => void
  title: string
  updatedAt: string
}

export function OwnProjectCard({
  isPrivate,
  onMenuClick,
  onOpenClick,
  title,
  updatedAt,
}: OwnProjectCardProps) {
  const actionLabel = isPrivate ? 'Manage private project' : 'Open project'
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
