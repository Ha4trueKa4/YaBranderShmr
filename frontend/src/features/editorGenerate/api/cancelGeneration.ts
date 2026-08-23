import { api, parseApiResponse } from '@shared/api/api'
import {
  type PostProjectsByIdGenerateByJobIdCancelResponse,
  zPostProjectsByIdGenerateByJobIdCancelResponse,
} from '@shared/api/types'

export async function cancelGeneration(
  projectId: string,
  jobId: string,
): Promise<PostProjectsByIdGenerateByJobIdCancelResponse> {
  const path = `/projects/${encodeURIComponent(projectId)}/generate/${encodeURIComponent(jobId)}/cancel`
  const response = await api<unknown>(path, {
    method: 'POST',
    requiresAuth: true,
  })

  return parseApiResponse(
    zPostProjectsByIdGenerateByJobIdCancelResponse,
    response,
    path,
  )
}
