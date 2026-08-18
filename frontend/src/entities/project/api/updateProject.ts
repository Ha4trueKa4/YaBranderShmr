import { api, parseApiResponse } from '@shared/api/api'
import { type PatchProjectsByIdResponse, zProject } from '@shared/api/types'

export interface UpdateProjectMetadataInput {
  projectId: string
  name?: string
  is_public?: boolean
  data?: string
}

export async function updateProject({
  projectId,
  name,
  is_public,
  data,
}: UpdateProjectMetadataInput): Promise<PatchProjectsByIdResponse> {
  const response = await api<unknown>(
    `/projects/${encodeURIComponent(projectId)}`,
    {
      method: 'PATCH',
      body: { data, is_public, name },
      requiresAuth: true,
    },
  )

  return parseApiResponse(zProject, response, `/projects/${projectId}`)
}
