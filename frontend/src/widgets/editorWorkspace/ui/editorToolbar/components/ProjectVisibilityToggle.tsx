import { Button, IconButton, Tooltip } from '@shared/ui'
import { Globe2, Lock } from 'lucide-react'
import { useTranslation } from 'react-i18next'

import styles from '../EditorToolbar.module.css'

interface ProjectVisibilityToggleProps {
  compact?: boolean
  disabled?: boolean
  isPublic: boolean
  onToggle: () => void
}

export function ProjectVisibilityToggle({
  compact = false,
  disabled = false,
  isPublic,
  onToggle,
}: ProjectVisibilityToggleProps) {
  const { t } = useTranslation()
  const tooltip = t(isPublic ? 'Make private' : 'Make public')
  const Icon = isPublic ? Globe2 : Lock

  if (compact) {
    return (
      <IconButton
        className={styles.mobileVisibilityToggle}
        disabled={disabled}
        onClick={onToggle}
        title={tooltip}
      >
        <Icon size={17} />
      </IconButton>
    )
  }

  return (
    <Tooltip content={tooltip}>
      <Button
        className={styles.visibilityToggle}
        disabled={disabled}
        onClick={onToggle}
        size="sm"
        variant="outline"
      >
        <Icon size={15} />
        {t(isPublic ? 'Public' : 'Private')}
      </Button>
    </Tooltip>
  )
}
