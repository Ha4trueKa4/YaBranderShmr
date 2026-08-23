import type { PostProjectsData } from '@shared/api/types'
import { useMutation } from '@tanstack/react-query'

import { createProject } from './createProject'

interface CreateProjectWithSignalInput {
  body: PostProjectsData['body']
  signal?: AbortSignal
}

type CreateProjectInput =
  | PostProjectsData['body']
  | CreateProjectWithSignalInput

export function useCreateProjectMutation() {
  return useMutation({
    mutationKey: ['project', 'create'],
    mutationFn: (input: CreateProjectInput) => {
      if ('body' in input) {
        return createProject(input.body, input.signal)
      }

      return createProject(input)
    },
  })
}
