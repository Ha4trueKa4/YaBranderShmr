import { useMutation } from '@tanstack/react-query'

import { createProject } from './createProject'

type CreateProjectRequest = Parameters<typeof createProject>[0]

interface CreateProjectWithSignalInput {
  body: CreateProjectRequest
  signal?: AbortSignal
}

type CreateProjectInput = CreateProjectRequest | CreateProjectWithSignalInput

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
