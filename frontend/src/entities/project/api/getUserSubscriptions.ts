import { api, parseApiResponse } from '@shared/api/api'
import {
  type GetUsersByIdSubscriptionsResponse,
  zGetUsersByIdSubscriptionsResponse,
} from '@shared/api/types'

export async function getUserSubscriptions(
  userId: string,
): Promise<GetUsersByIdSubscriptionsResponse> {
  const response = await api<unknown>(
    `/users/${encodeURIComponent(userId)}/subscriptions`,
  )

  return parseApiResponse(
    zGetUsersByIdSubscriptionsResponse,
    response,
    `/users/${userId}/subscriptions`,
  )
}
