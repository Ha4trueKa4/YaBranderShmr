import { api, parseApiResponse } from '@shared/api/api'
import { type Project, zGetProjectsByIdResponse } from '@shared/api/types'

export async function getProject(id: string): Promise<Project> {
  const response = await api<unknown>(`/projects/${encodeURIComponent(id)}`, {
    requiresAuth: true,
  })

  return parseApiResponse(zGetProjectsByIdResponse, response, `/projects/${id}`)
}
