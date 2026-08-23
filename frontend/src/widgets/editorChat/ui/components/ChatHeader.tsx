import { IconButton } from '@shared/ui'
import { GripHorizontal, Minimize2 } from 'lucide-react'
import type { PointerEvent } from 'react'
import { useTranslation } from 'react-i18next'
import styles from './ChatHeader.module.css'

interface ChatHeaderProps {
  onCollapse: () => void
  onPointerCancel: (event: PointerEvent<HTMLElement>) => void
  onPointerDown: (event: PointerEvent<HTMLElement>) => void
  onPointerMove: (event: PointerEvent<HTMLElement>) => void
  onPointerUp: (event: PointerEvent<HTMLElement>) => void
}

export function ChatHeader({
  onCollapse,
  onPointerCancel,
  onPointerDown,
  onPointerMove,
  onPointerUp,
}: ChatHeaderProps) {
  const { t } = useTranslation()

  return (
    <header
      className={styles.header}
      onPointerCancel={onPointerCancel}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={onPointerUp}
    >
      <span className={styles.title}>
        <GripHorizontal size={18} />
        {t('AI chat')}
      </span>
      <IconButton
        onClick={onCollapse}
        onPointerDown={(event) => event.stopPropagation()}
        size="sm"
        title={t('Collapse chat')}
      >
        <Minimize2 size={17} />
      </IconButton>
    </header>
  )
}
