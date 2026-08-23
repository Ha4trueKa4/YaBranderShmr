import type { ProjectChatMessage } from '@entities/projectChat'
import { ChatMessage } from '@shared/ui/ChatMessage'
import { useTranslation } from 'react-i18next'
import styles from './ChatMessageList.module.css'

interface ChatMessageListProps {
  messages: ProjectChatMessage[]
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
          isPending={
            message.kind === 'pending' || message.kind === 'cancelling'
          }
          key={message.id}
          pendingLabel={
            message.kind === 'cancelling'
              ? t('Cancelling generation…')
              : t('Updating the design…')
          }
        >
          {message.text ??
            (message.kind === 'success'
              ? t('A new version has been created.')
              : message.kind === 'cancelled'
                ? t('Generation cancelled.')
                : t('Unable to update the design. Please try again.'))}
        </ChatMessage>
      ))}
    </div>
  )
}
