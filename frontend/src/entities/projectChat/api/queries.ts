import { useQuery } from '@tanstack/react-query'
import { mapProjectChatMessages } from '../model/mapProjectChatMessage'
import { getProjectChat } from './getProjectChat'

const projectChatStaleTime = 30_000

export const projectChatQueryKey = (projectId: string) =>
  ['project', projectId, 'chat'] as const

export function useProjectChatQuery(projectId: string, enabled = true) {
  return useQuery({
    queryKey: projectChatQueryKey(projectId),
    queryFn: async () =>
      mapProjectChatMessages(await getProjectChat(projectId)),
    enabled: Boolean(projectId) && enabled,
    retry: false,
    staleTime: projectChatStaleTime,
  })
}
