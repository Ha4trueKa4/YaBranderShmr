import type { QueryClient } from '@tanstack/react-query'

import { generationStatusQueryOptions } from './queries'

export const GENERATION_POLL_TIMEOUT_MS = 5 * 60_000

export class GenerationPollingTimeoutError extends Error {
  constructor() {
    super('Generation polling timed out')
    this.name = 'GenerationPollingTimeoutError'
  }
}

export async function pollGenerationStatus(
  queryClient: QueryClient,
  projectId: string,
  jobId: string,
  timeoutMs = GENERATION_POLL_TIMEOUT_MS,
) {
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs)

  try {
    while (true) {
      const result = await queryClient.fetchQuery(
        generationStatusQueryOptions(projectId, jobId, controller.signal),
      )

      if (result.status !== 'pending') {
        return result
      }
    }
  } catch (error) {
    if (controller.signal.aborted) {
      throw new GenerationPollingTimeoutError()
    }

    throw error
  } finally {
    clearTimeout(timeoutId)
  }
}
