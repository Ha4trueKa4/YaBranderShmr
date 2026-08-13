import { Avatar } from '@shared/ui'

import { ProjectPreview } from '../projectPreview'
import styles from './FeedProjectCard.module.css'

export interface FeedProjectCardProps {
  authorInitials: string
  authorName: string
  authorTone?: 'accent' | 'cyan' | 'green' | 'rose'
  publishedAt: string
  title: string
}

export function FeedProjectCard({
  authorInitials,
  authorName,
  authorTone = 'accent',
  publishedAt,
  title,
}: FeedProjectCardProps) {
  return (
    <article className={styles.card}>
      <ProjectPreview title={title} />
      <div className={styles.content}>
        <h3>{title}</h3>
        <div className={styles.meta}>
          <div className={styles.author}>
            <Avatar
              className={styles[`avatar${capitalize(authorTone)}`]}
              initials={authorInitials}
              size="sm"
            />
            <span>{authorName}</span>
          </div>
          <time>{publishedAt}</time>
        </div>
      </div>
    </article>
  )
}

function capitalize(value: string) {
  return `${value[0].toUpperCase()}${value.slice(1)}`
}
