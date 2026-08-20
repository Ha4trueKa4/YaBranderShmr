import { useInfiniteQuery, useMutation, useQuery } from '@tanstack/react-query'

import { deleteProject } from './deleteProject'
import { type FeedScope, getFeed } from './getFeed'
import { getProject } from './getProject'
import { getProjectSvg } from './getProjectSvg'
import { getUserProfile } from './getProjects'
import { getUserSubscribers } from './getUserSubscribers'
import { getUserSubscriptions } from './getUserSubscriptions'
import { subscribeToUser } from './subscribeToUser'
import { unsubscribeFromUser } from './unsubscribeFromUser'
import { type UpdateProjectMetadataInput, updateProject } from './updateProject'
import {
  type UpdateUserProfileInput,
  updateUserProfile,
} from './updateUserProfile'

const projectQueryStaleTime = 60_000

export const feedQueryKey = (scope: FeedScope) => ['feed', scope] as const
export const projectQueryKey = (id: string) => ['project', id] as const
export const projectSvgQueryKey = (id: string) => ['project-svg', id] as const
export const userProfileQueryKey = (userId: string | undefined) =>
  ['user-profile', userId] as const
export const userSubscribersQueryKey = (userId: string | undefined) =>
  ['user-subscribers', userId] as const
export const userSubscriptionsQueryKey = (userId: string | undefined) =>
  ['user-subscriptions', userId] as const

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

export function useUpdateProjectMetadataMutation() {
  return useMutation({
    mutationKey: ['project', 'update-metadata'],
    mutationFn: (input: UpdateProjectMetadataInput) => updateProject(input),
  })
}

export function useUpdateUserProfileMutation() {
  return useMutation({
    mutationKey: ['user', 'update-profile'],
    mutationFn: (input: UpdateUserProfileInput) => updateUserProfile(input),
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

export function useUserSubscribersQuery(userId: string) {
  return useQuery({
    queryKey: userSubscribersQueryKey(userId),
    queryFn: () => getUserSubscribers(userId),
    enabled: Boolean(userId),
    retry: false,
  })
}

export function useUserSubscriptionsQuery(userId: string) {
  return useQuery({
    queryKey: userSubscriptionsQueryKey(userId),
    queryFn: () => getUserSubscriptions(userId),
    enabled: Boolean(userId),
    retry: false,
  })
}

export function useSubscribeToUserMutation() {
  return useMutation({
    mutationKey: ['user', 'subscribe'],
    mutationFn: (userId: string) => subscribeToUser(userId),
  })
}

export function useUnsubscribeFromUserMutation() {
  return useMutation({
    mutationKey: ['user', 'unsubscribe'],
    mutationFn: (userId: string) => unsubscribeFromUser(userId),
  })
}

export function useFeedInfiniteQuery(scope: FeedScope) {
  return useInfiniteQuery({
    queryKey: feedQueryKey(scope),
    initialPageParam: null as string | null,
    queryFn: ({ pageParam }) => getFeed(scope, pageParam),
    getNextPageParam: (lastPage) => lastPage.nextCursor ?? undefined,
    retry: false,
  })
}
