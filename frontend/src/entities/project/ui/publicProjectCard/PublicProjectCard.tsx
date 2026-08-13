import { ExternalLink } from 'lucide-react'

import { ProjectPreview } from '../projectPreview'
import styles from './PublicProjectCard.module.css'

export interface PublicProjectCardProps {
  description: string
  onOpenClick?: () => void
  title: string
}

export function PublicProjectCard({
  description,
  onOpenClick,
  title,
}: PublicProjectCardProps) {
  return (
    <article className={styles.card}>
      <ProjectPreview title={title} />
      <div className={styles.content}>
        <div className={styles.titleRow}>
          <h3>{title}</h3>
          <button
            className={styles.openButton}
            onClick={onOpenClick}
            type="button"
          >
            <ExternalLink size={18} />
          </button>
        </div>
        <p>{description}</p>
      </div>
    </article>
  )
}
