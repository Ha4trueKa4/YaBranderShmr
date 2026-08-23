export type ProjectChatMessageKind =
  | 'cancelled'
  | 'error'
  | 'pending'
  | 'cancelling'
  | 'prompt'
  | 'success'

export interface ProjectChatMessage {
  createdAt: string
  id: string
  jobId?: string
  kind: ProjectChatMessageKind
  role: 'assistant' | 'user'
  text?: string
  versionId?: string
}
