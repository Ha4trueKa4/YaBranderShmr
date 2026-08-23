import { ChatMessage } from '@shared/ui/ChatMessage'
import { useTranslation } from 'react-i18next'
import type { EditorChatMessage } from '../../model/types'
import styles from './ChatMessageList.module.css'

interface ChatMessageListProps {
  messages: EditorChatMessage[]
}

export function ChatMessageList({ messages }: ChatMessageListProps) {
  const { t } = useTranslation()

  if (!messages.length) {
    return (
      <div className={styles.emptyState}>
        {t('Describe the changes you want to make to the design.')}
      </div>
    )
  }

  return (
    <div className={styles.messages}>
      {messages.map((message) => (
        <ChatMessage
          author={message.role}
          isError={message.kind === 'error'}
          isPending={message.kind === 'pending'}
          key={message.id}
          pendingLabel={t('Updating the design…')}
        >
          {message.text}
        </ChatMessage>
      ))}
    </div>
  )
}
