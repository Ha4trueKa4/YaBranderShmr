import { api, parseApiResponse } from '@shared/api/api'
import {
  type PostProjectsData,
  type PostProjectsResponse,
  zPostProjectsResponse,
} from '@shared/api/types'

type CreateProjectRequest =
  | PostProjectsData['body']
  | {
      data: string
      name?: string
      references?: PostProjectsData['body']['references']
    }

export async function createProject(
  body: CreateProjectRequest,
  signal?: AbortSignal,
): Promise<PostProjectsResponse> {
  const response = await api<unknown, CreateProjectRequest>('/projects', {
    method: 'POST',
    body,
    requiresAuth: true,
    signal,
  })

  return parseApiResponse(zPostProjectsResponse, response, '/projects')
}
