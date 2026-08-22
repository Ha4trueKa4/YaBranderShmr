import { api, parseApiResponse } from '@shared/api/api'
import {
  type GetProjectsByIdGenerateByJobIdResponse,
  zGetProjectsByIdGenerateByJobIdResponse,
} from '@shared/api/types'

export async function getGenerationStatus(
  projectId: string,
  jobId: string,
  signal?: AbortSignal,
): Promise<GetProjectsByIdGenerateByJobIdResponse> {
  const path = `/projects/${encodeURIComponent(projectId)}/generate/${encodeURIComponent(jobId)}`
  const response = await api<unknown>(path, {
    method: 'GET',
    requiresAuth: true,
    signal,
  })

  return parseApiResponse(
    zGetProjectsByIdGenerateByJobIdResponse,
    response,
    path,
  )
}
