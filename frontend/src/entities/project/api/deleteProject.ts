import { api, parseApiResponse } from '@shared/api/api'
import {
  type DeleteProjectsByIdResponse,
  zDeleteProjectsByIdResponse,
} from '@shared/api/types'

export async function deleteProject(
  projectId: string,
): Promise<DeleteProjectsByIdResponse> {
  const response = await api<unknown>(
    `/projects/${encodeURIComponent(projectId)}`,
    { method: 'DELETE', requiresAuth: true },
  )

  return parseApiResponse(
    zDeleteProjectsByIdResponse,
    response,
    `/projects/${projectId}`,
  )
}
