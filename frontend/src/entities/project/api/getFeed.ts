import { api, parseApiResponse } from '@shared/api/api'
import {
  type FeedResponse,
  type GetFeedData,
  zGetFeedResponse,
} from '@shared/api/types'
import i18n from '@/i18n'

import type { FeedProject } from '../model/types'

const feedPageSize = 40

export type FeedScope = Exclude<
  NonNullable<GetFeedData['query']>['scope'],
  undefined
>

export interface FeedPage {
  items: FeedProject[]
  nextCursor: string | null
}

export async function getFeed(
  scope: FeedScope,
  cursor?: string | null,
): Promise<FeedPage> {
  const response = await api<unknown>('/feed', {
    query: { cursor, limit: feedPageSize, scope },
  })
  const feed = parseApiResponse(zGetFeedResponse, response, '/feed')

  return {
    items: mapFeedItems(feed),
    nextCursor: feed.next_cursor ?? null,
  }
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
    return i18n.t('Recently')
  }

  const createdAtTime = new Date(createdAt).getTime()
  const elapsedTime = Date.now() - createdAtTime

  if (Number.isNaN(createdAtTime) || elapsedTime < 0) {
    return i18n.t('Recently')
  }

  const minutes = Math.floor(elapsedTime / 60_000)

  if (minutes < 60) {
    return minutes < 1
      ? i18n.t('Just now')
      : i18n.t('{{count}}m ago', { count: minutes })
  }

  const hours = Math.floor(minutes / 60)

  if (hours < 24) {
    return i18n.t('{{count}}h ago', { count: hours })
  }

  return i18n.t('{{count}}d ago', { count: Math.floor(hours / 24) })
}
