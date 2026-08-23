import type { PostProjectsByIdGenerateData } from '@shared/api/types'
import { queryOptions, useMutation, useQuery } from '@tanstack/react-query'

import { generateProject } from './generateProject'
import { getGenerationStatus } from './getGenerationStatus'
import { getModels } from './getModels'

interface GenerateProjectInput {
  model: PostProjectsByIdGenerateData['body']['model']
  projectId: string
  prompt: PostProjectsByIdGenerateData['body']['prompt']
  signal?: AbortSignal
}

export const modelsQueryKey = ['models'] as const
export const generationStatusQueryKey = (projectId: string, jobId: string) =>
  ['project', projectId, 'generation', jobId] as const

export function generationStatusQueryOptions(
  projectId: string,
  jobId: string,
  signal?: AbortSignal,
) {
  return queryOptions({
    queryKey: generationStatusQueryKey(projectId, jobId),
    queryFn: () => getGenerationStatus(projectId, jobId, signal),
    retry: false,
    staleTime: 0,
  })
}

export function useModelsQuery() {
  return useQuery({
    queryKey: modelsQueryKey,
    queryFn: getModels,
    retry: false,
    staleTime: 5 * 60_000,
  })
}

export function useGenerateProjectMutation() {
  return useMutation({
    mutationKey: ['project', 'generate'],
    mutationFn: ({ model, projectId, prompt, signal }: GenerateProjectInput) =>
      generateProject(projectId, { model, prompt }, signal),
  })
}
