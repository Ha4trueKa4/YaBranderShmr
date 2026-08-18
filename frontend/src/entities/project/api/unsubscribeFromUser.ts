import { api, parseApiResponse } from '@shared/api/api'
import {
  type DeleteUsersByIdSubscribeResponse,
  zDeleteUsersByIdSubscribeResponse,
} from '@shared/api/types'

export async function unsubscribeFromUser(
  userId: string,
): Promise<DeleteUsersByIdSubscribeResponse> {
  const response = await api<unknown>(
    `/users/${encodeURIComponent(userId)}/subscribe`,
    { method: 'DELETE', requiresAuth: true },
  )

  return parseApiResponse(
    zDeleteUsersByIdSubscribeResponse,
    response,
    `/users/${userId}/subscribe`,
  )
}
