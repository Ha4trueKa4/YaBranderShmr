import { api, parseApiResponse } from '@shared/api/api'
import {
  type GetUsersByIdSubscribersResponse,
  zGetUsersByIdSubscribersResponse,
} from '@shared/api/types'

export async function getUserSubscribers(
  userId: string,
): Promise<GetUsersByIdSubscribersResponse> {
  const response = await api<unknown>(
    `/users/${encodeURIComponent(userId)}/subscribers`,
    { requiresAuth: true },
  )

  return parseApiResponse(
    zGetUsersByIdSubscribersResponse,
    response,
    `/users/${userId}/subscribers`,
  )
}
