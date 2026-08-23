import type { ChatMessage } from '@shared/api/types'
import type { ProjectChatMessage } from './types'

function createMessageId(message: ChatMessage, index: number) {
  return [
    message.job_id ?? 'message',
    message.role,
    message.created_at,
    index,
  ].join(':')
}

export function mapProjectChatMessage(
  message: ChatMessage,
  index: number,
): ProjectChatMessage | null {
  const id = createMessageId(message, index)

  if (message.role === 'user') {
    if (!message.text) {
      return null
    }

    return {
      createdAt: message.created_at,
      id,
      jobId: message.job_id,
      kind: 'prompt',
      role: 'user',
      text: message.text,
    }
  }

  if (message.version_id) {
    return {
      createdAt: message.created_at,
      id,
      jobId: message.job_id,
      kind: 'success',
      role: 'assistant',
      versionId: message.version_id,
    }
  }

  if (message.error) {
    return {
      createdAt: message.created_at,
      id,
      jobId: message.job_id,
      kind: 'error',
      role: 'assistant',
    }
  }

  if (message.status) {
    return {
      createdAt: message.created_at,
      id,
      jobId: message.job_id,
      kind: message.status,
      role: 'assistant',
    }
  }

  return null
}

export function mapProjectChatMessages(messages: ChatMessage[]) {
  return messages.flatMap((message, index) => {
    const mappedMessage = mapProjectChatMessage(message, index)

    return mappedMessage ? [mappedMessage] : []
  })
}
