import { api, parseApiResponse } from '@shared/api/api'
import {
  type GetUsersByIdResponse,
  zGetUsersByIdResponse,
} from '@shared/api/types'

export async function getUserProfile(
  userId: string,
): Promise<GetUsersByIdResponse> {
  const response = await api<unknown>(`/users/${encodeURIComponent(userId)}`)

  return parseApiResponse(zGetUsersByIdResponse, response, `/users/${userId}`)
}
