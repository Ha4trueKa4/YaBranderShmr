import { api, parseApiResponse } from '@shared/api/api'
import type { VersionInfo, VersionUrl } from '@shared/api/types'
import {
  zGetProjectsByIdVersionsResponse,
  zVersionUrl,
} from '@shared/api/types/schemas'

export async function getProjectVersions(
  projectId: string,
): Promise<VersionInfo[]> {
  const response = await api<unknown>(
    `/projects/${encodeURIComponent(projectId)}/versions`,
    { requiresAuth: true },
  )
  const data = parseApiResponse(
    zGetProjectsByIdVersionsResponse,
    response,
    `/projects/${projectId}/versions`,
  )

  return data.versions ?? []
}

export async function getProjectVersion(
  projectId: string,
  versionId: string,
): Promise<VersionUrl> {
  const response = await api<unknown>(
    `/projects/${encodeURIComponent(projectId)}/versions/${encodeURIComponent(versionId)}`,
    { requiresAuth: true },
  )

  return parseApiResponse(
    zVersionUrl,
    response,
    `/projects/${projectId}/versions/${versionId}`,
  )
}

export async function restoreProjectVersion(
  projectId: string,
  versionId: string,
): Promise<VersionUrl> {
  const response = await api<unknown>(
    `/projects/${encodeURIComponent(projectId)}/versions/${encodeURIComponent(versionId)}/restore`,
    { body: {}, method: 'POST', requiresAuth: true },
  )

  return parseApiResponse(
    zVersionUrl,
    response,
    `/projects/${projectId}/versions/${versionId}/restore`,
  )
}
