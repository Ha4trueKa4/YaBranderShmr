import { api, parseApiResponse } from '@shared/api/api'
import {
  type ChatMessage,
  zGetProjectsByIdChatResponse,
} from '@shared/api/types'

export async function getProjectChat(
  projectId: string,
): Promise<ChatMessage[]> {
  const path = `/projects/${encodeURIComponent(projectId)}/chat`
  const response = await api<unknown>(path, { requiresAuth: true })
  const data = parseApiResponse(zGetProjectsByIdChatResponse, response, path)

  return data.chat_history ?? []
}
