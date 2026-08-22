import type { Project } from '@shared/api/types'
import { Button, IconButton, PopupMenu, Spinner } from '@shared/ui'
import { MoreHorizontal } from 'lucide-react'
import type { KeyboardEvent, MouseEvent } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'

import { ProjectPreview } from '../projectPreview'
import styles from './OwnProjectCard.module.css'

export interface OwnProjectCardProps {
  isGenerating?: boolean
  onDeleteClick?: () => void
  onEditClick?: () => void
  onOpenClick?: () => void
  project: Project
}

export function OwnProjectCard({
  isGenerating = false,
  onDeleteClick,
  onEditClick,
  onOpenClick,
  project,
}: OwnProjectCardProps) {
  const { i18n, t } = useTranslation()
  const [isMenuOpen, setMenuOpen] = useState(false)
  const isPrivate = !project.is_public
  const title = project.name ?? t('Untitled project')
  const updatedAt = getProjectStatus(project, i18n.language, t)
  const visibility = isPrivate ? t('Private') : t('Public')

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
        {isGenerating ? (
          <div
            aria-label={t('Generating project')}
            className={styles.generationPreview}
            role="status"
          >
            <Spinner />
          </div>
        ) : (
          <ProjectPreview imageUrl={project.url ?? null} title={title} />
        )}
        {!isPrivate && <span className={styles.visibility}>{visibility}</span>}
      </div>
      <div className={styles.content}>
        <div className={styles.titleRow}>
          <h3>{title}</h3>
          <IconButton
            onClick={handleMenuClick}
            onPointerDown={(event) => event.stopPropagation()}
            size="sm"
            title={t('Project actions')}
          >
            <MoreHorizontal size={20} />
          </IconButton>
          {isMenuOpen && (
            <PopupMenu
              items={[
                { label: t('Edit project'), onClick: () => onEditClick?.() },
                {
                  label: t('Delete project'),
                  onClick: () => onDeleteClick?.(),
                },
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
          {t('Open project')}
        </Button>
      </div>
    </article>
  )
}

function getProjectStatus(
  project: Project,
  language: string,
  t: (key: string) => string,
) {
  if (!project.is_public) {
    return t('Draft')
  }

  if (!project.created_at) {
    return t('Published')
  }

  return `${t('Published')} ${new Intl.DateTimeFormat(language, {
    day: 'numeric',
    month: 'short',
  }).format(new Date(project.created_at))}`
}
