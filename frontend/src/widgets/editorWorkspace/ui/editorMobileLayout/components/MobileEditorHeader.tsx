import { IconButton, PopupMenu } from '@shared/ui'
import { ArrowLeft } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'

import styles from '../EditorMobileLayout.module.css'

interface MobileEditorHeaderProps {
  canEdit: boolean
  isCreatingProjectCopy: boolean
  isSavingVersion: boolean
  onBack: () => void
  onCreateCopy: () => void
  onExport: () => void
  onOpenVersionHistory: () => void
  onSaveVersion: () => void
  projectName: string
}

export function MobileEditorHeader({
  canEdit,
  isCreatingProjectCopy,
  isSavingVersion,
  onBack,
  onCreateCopy,
  onExport,
  onOpenVersionHistory,
  onSaveVersion,
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
        <strong>{projectName}</strong>
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
              { label: 'Export', onClick: onExport },
              ...(canEdit
                ? [
                    {
                      label: isSavingVersion ? t('Saving…') : t('Save'),
                      onClick: onSaveVersion,
                    },
                    {
                      label: t('History'),
                      onClick: onOpenVersionHistory,
                    },
                  ]
                : [
                    {
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
