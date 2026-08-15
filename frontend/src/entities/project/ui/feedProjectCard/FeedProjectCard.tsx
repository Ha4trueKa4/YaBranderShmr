import type { FeedProject } from '@entities/project/model/types'
import { getAvatarInitials } from '@shared/lib'
import { Avatar } from '@shared/ui'
import { Link } from 'react-router-dom'

import { ProjectPreview } from '../projectPreview'
import styles from './FeedProjectCard.module.css'

export interface FeedProjectCardProps {
  project: FeedProject
}

export function FeedProjectCard({ project }: FeedProjectCardProps) {
  const authorInitials = getAvatarInitials(project.author.name)

  return (
    <article className={styles.card}>
      <ProjectPreview imageUrl={project.previewUrl} title={project.name} />
      <div className={styles.content}>
        <h3>{project.name}</h3>
        <div className={styles.meta}>
          <div className={styles.author}>
            <Link
              className={styles.avatarLink}
              to={`/users/${project.author.id}`}
            >
              <Avatar initials={authorInitials} size="sm" />
            </Link>
            <span>{project.author.name}</span>
          </div>
          <time>{project.publishedAt}</time>
        </div>
      </div>
    </article>
  )
}
