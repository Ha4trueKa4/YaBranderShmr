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

import styles from '../EditorMobileLayout.module.css'

interface MobileEditorHeaderProps {
  canEdit: boolean
  isCreatingProjectCopy: boolean
  isSavingVersion: boolean
  onBack: () => void
  onCreateCopy: () => void
  onExport: () => void
  onOpenVersionHistory: () => void
  onOpenChat: () => void
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
  onOpenChat,
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
