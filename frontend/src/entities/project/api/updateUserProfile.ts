import { api, parseApiResponse } from '@shared/api/api'
import {
  type PatchUsersByIdResponse,
  zPatchUsersByIdResponse,
} from '@shared/api/types'

export interface UpdateUserProfileInput {
  nickname: string
  userId: string
}

export async function updateUserProfile({
  nickname,
  userId,
}: UpdateUserProfileInput): Promise<PatchUsersByIdResponse> {
  const response = await api<unknown>(`/users/${encodeURIComponent(userId)}`, {
    method: 'PATCH',
    body: { nickname },
    requiresAuth: true,
  })

  return parseApiResponse(zPatchUsersByIdResponse, response, `/users/${userId}`)
}
