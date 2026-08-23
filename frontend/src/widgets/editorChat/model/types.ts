export interface EditorChatMessage {
  id: string
  kind: 'error' | 'pending' | 'text'
  role: 'assistant' | 'user'
  text?: string
}

export interface EditorChatPosition {
  x: number
  y: number
}
