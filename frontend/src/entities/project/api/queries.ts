import { useMutation, useQuery } from '@tanstack/react-query'

import { deleteProject } from './deleteProject'
import { type FeedScope, getFeed } from './getFeed'
import { getProject } from './getProject'
import { getProjectSvg } from './getProjectSvg'
import { getUserProfile } from './getProjects'

const projectQueryStaleTime = 60_000

export const feedQueryKey = (scope: FeedScope) => ['feed', scope] as const
export const projectQueryKey = (id: string) => ['project', id] as const
export const projectSvgQueryKey = (id: string) => ['project-svg', id] as const
export const userProfileQueryKey = (userId: string | undefined) =>
  ['user-profile', userId] as const

export function useProjectQuery(id: string) {
  return useQuery({
    queryKey: projectQueryKey(id),
    queryFn: () => getProject(id),
    enabled: Boolean(id),
    retry: false,
    staleTime: projectQueryStaleTime,
  })
}

export function useProjectSvgQuery(id: string, url: string | undefined) {
  return useQuery({
    queryKey: projectSvgQueryKey(id),
    queryFn: () => {
      if (!url) {
        throw new TypeError('Project SVG URL is missing')
      }

      return getProjectSvg(url)
    },
    enabled: Boolean(id && url),
    retry: false,
    staleTime: projectQueryStaleTime,
  })
}

export function useDeleteProjectMutation() {
  return useMutation({
    mutationKey: ['project', 'delete'],
    mutationFn: (projectId: string) => deleteProject(projectId),
  })
}

export function useUserProfileQuery(userId: string) {
  return useQuery({
    queryKey: userProfileQueryKey(userId),
    queryFn: () => getUserProfile(userId),
    enabled: Boolean(userId),
    retry: false,
  })
}

export function useFeedQuery(scope: FeedScope) {
  return useQuery({
    queryKey: feedQueryKey(scope),
    queryFn: () => getFeed(scope),
    retry: false,
  })
}
