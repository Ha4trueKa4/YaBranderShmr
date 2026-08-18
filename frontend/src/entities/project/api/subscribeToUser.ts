import { api, parseApiResponse } from '@shared/api/api'
import {
  type PostUsersByIdSubscribeResponse,
  zPostUsersByIdSubscribeResponse,
} from '@shared/api/types'

export async function subscribeToUser(
  userId: string,
): Promise<PostUsersByIdSubscribeResponse> {
  const response = await api<unknown>(
    `/users/${encodeURIComponent(userId)}/subscribe`,
    { method: 'POST' },
  )

  return parseApiResponse(
    zPostUsersByIdSubscribeResponse,
    response,
    `/users/${userId}/subscribe`,
  )
}
