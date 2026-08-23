import { Spinner } from '../Spinner'
import styles from './ChatMessage.module.css'

export type ChatMessageAuthor = 'assistant' | 'user'

export interface ChatMessageProps {
  author: ChatMessageAuthor
  children?: string
  isError?: boolean
  isPending?: boolean
  pendingLabel?: string
}

export function ChatMessage({
  author,
  children,
  isError = false,
  isPending = false,
  pendingLabel,
}: ChatMessageProps) {
  return (
    <article
      className={[
        styles.message,
        styles[author],
        isError && styles.error,
        isPending && styles.pending,
      ]
        .filter(Boolean)
        .join(' ')}
    >
      {isPending ? (
        <span className={styles.pendingContent}>
          <Spinner size="sm" />
          {pendingLabel}
        </span>
      ) : (
        children
      )}
    </article>
  )
}
