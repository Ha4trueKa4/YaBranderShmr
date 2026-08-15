import { api } from '@shared/api/api'
import { type Project, zGetUsersByIdResponse } from '@shared/api/types'

export async function getProjects(userId: string): Promise<Project[]> {
  const response = await api<unknown>(`/users/${encodeURIComponent(userId)}`)
  const user = zGetUsersByIdResponse.parse(response)

  return user.projects ?? []
}
