import type { PostProjectsByIdGenerateData } from '@shared/api/types'
import { useMutation } from '@tanstack/react-query'

import { generateProject } from './generateProject'

interface GenerateProjectInput {
  projectId: string
  prompt: PostProjectsByIdGenerateData['body']['prompt']
}

export function useGenerateProjectMutation() {
  return useMutation({
    mutationKey: ['project', 'generate'],
    mutationFn: ({ projectId, prompt }: GenerateProjectInput) =>
      generateProject(projectId, { prompt }),
  })
}
