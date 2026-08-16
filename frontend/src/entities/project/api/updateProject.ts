import { api, parseApiResponse } from '@shared/api/api'
import { type PatchProjectsByIdResponse, zProject } from '@shared/api/types'

export interface UpdateProjectMetadataInput {
  projectId: string
  is_public: boolean
}

export async function updateProject({
  projectId,
  is_public,
}: UpdateProjectMetadataInput): Promise<PatchProjectsByIdResponse> {
  const response = await api<unknown>(
    `/projects/${encodeURIComponent(projectId)}`,
    {
      method: 'PATCH',
      body: { is_public },
    },
  )

  return parseApiResponse(zProject, response, `/projects/${projectId}`)
}
