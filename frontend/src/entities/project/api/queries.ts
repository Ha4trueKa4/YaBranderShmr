import { useQuery } from '@tanstack/react-query'

import { getProject } from './getProject'
import { getProjects } from './getProjects'

export const projectQueryKey = (id: string) => ['project', id] as const
export const projectsQueryKey = ['projects'] as const

export function useProjectQuery(id: string) {
  return useQuery({
    queryKey: projectQueryKey(id),
    queryFn: () => getProject(id),
    enabled: Boolean(id),
    retry: false,
  })
}

export function useProjectsQuery() {
  return useQuery({
    queryKey: projectsQueryKey,
    queryFn: getProjects,
    retry: false,
  })
}
