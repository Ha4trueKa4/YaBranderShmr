import type { PublicProfileProject } from '@entities/project/model/types'
import { ExternalLink } from 'lucide-react'

import { ProjectPreview } from '../projectPreview'
import styles from './PublicProjectCard.module.css'

export interface PublicProjectCardProps {
  onOpenClick?: () => void
  project: PublicProfileProject
}

export function PublicProjectCard({
  onOpenClick,
  project,
}: PublicProjectCardProps) {
  return (
    <article className={styles.card}>
      <ProjectPreview imageUrl={project.previewUrl} title={project.name} />
      <div className={styles.content}>
        <div className={styles.titleRow}>
          <h3>{project.name}</h3>
          <button
            className={styles.openButton}
            onClick={onOpenClick}
            type="button"
          >
            <ExternalLink size={18} />
          </button>
        </div>
        <p>{project.description}</p>
      </div>
    </article>
  )
}
