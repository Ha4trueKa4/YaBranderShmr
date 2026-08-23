import { IconButton, PopupMenu } from '@shared/ui'
import {
  ArrowLeft,
  Download,
  History,
  MessageCircle,
  Save,
  SquarePen,
} from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'

import { ProjectVisibilityToggle } from '../../editorToolbar/components/ProjectVisibilityToggle'
import styles from '../EditorMobileLayout.module.css'

interface MobileEditorHeaderProps {
  canEdit: boolean
  isCreatingProjectCopy: boolean
  isProjectPublic: boolean
  isSavingVersion: boolean
  isVisibilityUpdating: boolean
  onBack: () => void
  onCreateCopy: () => void
  onExport: () => void
  onOpenVersionHistory: () => void
  onOpenChat: () => void
  onSaveVersion: () => void
  onToggleProjectVisibility: () => void
  projectName: string
}

export function MobileEditorHeader({
  canEdit,
  isCreatingProjectCopy,
  isProjectPublic,
  isSavingVersion,
  isVisibilityUpdating,
  onBack,
  onCreateCopy,
  onExport,
  onOpenVersionHistory,
  onOpenChat,
  onSaveVersion,
  onToggleProjectVisibility,
  projectName,
}: MobileEditorHeaderProps) {
  const [isProjectMenuOpen, setIsProjectMenuOpen] = useState(false)
  const { t } = useTranslation()

  return (
    <header className={styles.header}>
      <IconButton className={styles.headerButton} onClick={onBack}>
        <ArrowLeft className={styles.backIcon} />
      </IconButton>
      <div className={styles.title}>
        <strong title={projectName}>{projectName}</strong>
        {canEdit && (
          <ProjectVisibilityToggle
            compact
            disabled={isVisibilityUpdating}
            isPublic={isProjectPublic}
            onToggle={onToggleProjectVisibility}
          />
        )}
      </div>
      <div
        className={styles.menuAnchor}
        onPointerDown={(event) => event.stopPropagation()}
      >
        <IconButton
          className={styles.headerButton}
          onClick={() => setIsProjectMenuOpen((isOpen) => !isOpen)}
        >
          <span className={styles.moreIcon}>•••</span>
        </IconButton>
        {isProjectMenuOpen && (
          <PopupMenu
            items={[
              {
                icon: <Download size={15} />,
                label: 'Export',
                onClick: onExport,
              },
              ...(canEdit
                ? [
                    {
                      icon: <Save size={15} />,
                      label: isSavingVersion ? t('Saving…') : t('Save'),
                      onClick: onSaveVersion,
                    },
                    {
                      icon: <History size={15} />,
                      label: t('History'),
                      onClick: onOpenVersionHistory,
                    },
                    {
                      icon: <MessageCircle size={15} />,
                      label: t('AI chat'),
                      onClick: onOpenChat,
                    },
                  ]
                : [
                    {
                      icon: <SquarePen size={15} />,
                      label: isCreatingProjectCopy ? 'Opening…' : 'Explore',
                      onClick: onCreateCopy,
                    },
                  ]),
            ]}
            onClose={() => setIsProjectMenuOpen(false)}
          />
        )}
      </div>
    </header>
  )
}
