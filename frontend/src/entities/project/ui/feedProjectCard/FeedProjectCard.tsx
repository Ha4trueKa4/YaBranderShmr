import type { FeedProject } from '@entities/project/model/types'
import { getAvatarInitials } from '@shared/lib'
import { Avatar, Button } from '@shared/ui'
import { Check, Plus } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'

import { ProjectPreview } from '../projectPreview'
import styles from './FeedProjectCard.module.css'

export interface FeedProjectCardProps {
  canAddReference?: boolean
  isReferenceSelected?: boolean
  onReferenceToggle?: (project: FeedProject) => void
  project: FeedProject
}

export function FeedProjectCard({
  canAddReference = false,
  isReferenceSelected = false,
  onReferenceToggle,
  project,
}: FeedProjectCardProps) {
  const { t } = useTranslation()
  const authorInitials = getAvatarInitials(project.author.name)
  const isReferenceLimitReached = !isReferenceSelected && !canAddReference

  return (
    <article className={styles.card}>
      <ProjectPreview
        className={styles.preview}
        imageUrl={project.previewUrl}
        title={project.name}
      />
      <div className={styles.content}>
        <h3>
          <Link className={styles.projectLink} to={`/projects/${project.id}`}>
            {project.name}
          </Link>
        </h3>
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
        <Button
          className={`${styles.referenceButton} ${
            isReferenceSelected ? styles.referenceButtonSelected : ''
          }`}
          disabled={isReferenceLimitReached}
          onClick={(event) => {
            event.preventDefault()
            event.stopPropagation()
            onReferenceToggle?.(project)
          }}
          size="sm"
          variant={isReferenceSelected ? 'primary' : 'outline'}
        >
          {isReferenceSelected ? <Check size={16} /> : <Plus size={16} />}
          {isReferenceSelected
            ? t('Remove reference')
            : isReferenceLimitReached
              ? t('Reference limit reached')
              : t('Add reference')}
        </Button>
      </div>
    </article>
  )
}
