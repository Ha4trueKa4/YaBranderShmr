import { IconButton } from '@shared/ui'
import { Expand, MessageCircle } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import styles from './ChatCollapsedBar.module.css'
import { ChatComposer } from './ChatComposer'

interface ChatCollapsedBarProps {
  disabled?: boolean
  onDraftChange: (nextValue: string) => void
  onExpand: () => void
  onSubmit: () => void
  prompt: string
}

export function ChatCollapsedBar({
  disabled = false,
  onDraftChange,
  onExpand,
  onSubmit,
  prompt,
}: ChatCollapsedBarProps) {
  const { t } = useTranslation()

  return (
    <div className={styles.bar}>
      <MessageCircle className={styles.icon} size={19} />
      <ChatComposer
        disabled={disabled}
        onChange={onDraftChange}
        onSubmit={onSubmit}
        placeholder={t('Ask AI to change the design…')}
        value={prompt}
      />
      <IconButton onClick={onExpand} title={t('Expand chat')}>
        <Expand size={18} />
      </IconButton>
    </div>
  )
}
