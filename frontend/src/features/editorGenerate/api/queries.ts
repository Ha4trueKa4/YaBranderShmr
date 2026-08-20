import type { PostProjectsByIdGenerateData } from '@shared/api/types'
import { useMutation, useQuery } from '@tanstack/react-query'

import { generateProject } from './generateProject'
import { getModels } from './getModels'

interface GenerateProjectInput {
  model: PostProjectsByIdGenerateData['body']['model']
  projectId: string
  prompt: PostProjectsByIdGenerateData['body']['prompt']
}

export const modelsQueryKey = ['models'] as const

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
    mutationFn: ({ model, projectId, prompt }: GenerateProjectInput) =>
      generateProject(projectId, { model, prompt }),
  })
}
