import { api } from '@shared/api/api'
import {
  zPostProjectsBody,
  zPostProjectsResponse,
  type PostProjectsData,
  type Project,
} from '@shared/api/types'

type CreateProjectRequest = PostProjectsData['body']

export async function createProject(
  body: CreateProjectRequest,
): Promise<Project> {
  const validBody = zPostProjectsBody.parse(body)
  const response = await api<unknown, CreateProjectRequest>('/projects', {
    method: 'POST',
    body: validBody,
  })

  return zPostProjectsResponse.parse(response)
}
