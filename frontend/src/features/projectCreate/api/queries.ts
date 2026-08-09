import type { PostProjectsData } from '@shared/api/types'
import { useMutation } from '@tanstack/react-query'

import { createProject } from './createProject'

type CreateProjectRequest = PostProjectsData['body']

export function useCreateProjectMutation() {
  return useMutation({
    mutationKey: ['project', 'create'],
    mutationFn: (body: CreateProjectRequest) => createProject(body),
  })
}
