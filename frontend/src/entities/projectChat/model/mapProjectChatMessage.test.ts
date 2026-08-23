import type { ChatMessage } from '@shared/api/types'
import { describe, expect, it } from 'vitest'
import {
  mapProjectChatMessage,
  mapProjectChatMessages,
} from './mapProjectChatMessage'

const createdAt = '2026-08-23T10:00:00.000000+00:00'

describe('mapProjectChatMessage', () => {
  it('maps a user prompt', () => {
    const message: ChatMessage = {
      created_at: createdAt,
      job_id: 'job-1',
      role: 'user',
      text: 'Make the background lighter',
    }

    expect(mapProjectChatMessage(message, 0)).toMatchObject({
      jobId: 'job-1',
      kind: 'prompt',
      role: 'user',
      text: 'Make the background lighter',
    })
  })

  it('maps a successful assistant response to a version reference', () => {
    const message: ChatMessage = {
      created_at: createdAt,
      job_id: 'job-1',
      role: 'assistant',
      version_id: 'version-1',
    }

    expect(mapProjectChatMessage(message, 1)).toMatchObject({
      kind: 'success',
      versionId: 'version-1',
    })
  })

  it('does not expose server error text in the UI model', () => {
    const message: ChatMessage = {
      created_at: createdAt,
      error: 'Internal provider error with implementation details',
      role: 'assistant',
    }

    expect(mapProjectChatMessage(message, 0)).toEqual({
      createdAt,
      id: `message:assistant:${createdAt}:0`,
      kind: 'error',
      role: 'assistant',
    })
  })

  it('maps the server pending placeholder', () => {
    const message: ChatMessage = {
      created_at: createdAt,
      job_id: 'job-1',
      role: 'assistant',
      status: 'pending',
    }

    expect(mapProjectChatMessage(message, 0)).toMatchObject({
      jobId: 'job-1',
      kind: 'pending',
    })
  })

  it('skips malformed history entries', () => {
    const messages: ChatMessage[] = [
      { created_at: createdAt, role: 'user' },
      { created_at: createdAt, role: 'assistant' },
    ]

    expect(mapProjectChatMessages(messages)).toEqual([])
  })
})
