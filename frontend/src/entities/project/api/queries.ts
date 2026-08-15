import { useQuery } from '@tanstack/react-query'

import { type FeedScope, getFeed } from './getFeed'
import { getProject } from './getProject'
import { getUserProfile } from './getProjects'

export const feedQueryKey = (scope: FeedScope) => ['feed', scope] as const
export const projectQueryKey = (id: string) => ['project', id] as const
export const userProfileQueryKey = (userId: string | undefined) =>
  ['user-profile', userId] as const

export function useProjectQuery(id: string) {
  return useQuery({
    queryKey: projectQueryKey(id),
    queryFn: () => getProject(id),
    enabled: Boolean(id),
    retry: false,
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
