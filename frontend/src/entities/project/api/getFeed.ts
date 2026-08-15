import { api, parseApiResponse } from '@shared/api/api'
import {
  type FeedResponse,
  type GetFeedData,
  zGetFeedResponse,
} from '@shared/api/types'

import type { FeedProject } from '../model/types'

export type FeedScope = Exclude<
  NonNullable<GetFeedData['query']>['scope'],
  undefined
>

export interface FeedPage {
  items: FeedProject[]
  nextCursor: string | null
}

export async function getFeed(scope: FeedScope): Promise<FeedPage> {
  const response = await api<unknown>('/feed', {
    query: { limit: 20, scope },
  })
  const feed = parseApiResponse(
    zGetFeedResponse,
    normalizeFeedResponse(response),
    '/feed',
  )

  return {
    items: mapFeedItems(feed),
    nextCursor: feed.next_cursor ?? null,
  }
}

function normalizeFeedResponse(response: unknown): unknown {
  if (!isRecord(response) || !Array.isArray(response.items)) {
    return response
  }

  return {
    ...response,
    items: response.items.map(normalizeFeedItem),
  }
}

function normalizeFeedItem(item: unknown): unknown {
  if (!isRecord(item) || typeof item.created_at !== 'string') {
    return item
  }

  // FIXME: Remove after the API returns RFC 3339 offsets (for example, +00:00).
  return {
    ...item,
    created_at: item.created_at.replace(/([+-]\d{2})(\d{2})$/, '$1:$2'),
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function mapFeedItems(feed: FeedResponse): FeedProject[] {
  return (feed.items ?? []).flatMap((item) => {
    if (!item.id || !item.author_id || !item.author_login || !item.name) {
      return []
    }

    return [
      {
        author: {
          avatarUrl: null,
          id: item.author_id,
          name: item.author_login,
        },
        id: item.id,
        name: item.name,
        previewUrl: item.preview_url || null,
        publishedAt: formatPublishedAt(item.created_at),
      },
    ]
  })
}

function formatPublishedAt(createdAt: string | undefined) {
  if (!createdAt) {
    return 'Recently'
  }

  const createdAtTime = new Date(createdAt).getTime()
  const elapsedTime = Date.now() - createdAtTime

  if (Number.isNaN(createdAtTime) || elapsedTime < 0) {
    return 'Recently'
  }

  const minutes = Math.floor(elapsedTime / 60_000)

  if (minutes < 60) {
    return minutes < 1 ? 'Just now' : `${minutes}m ago`
  }

  const hours = Math.floor(minutes / 60)

  if (hours < 24) {
    return `${hours}h ago`
  }

  return `${Math.floor(hours / 24)}d ago`
}
