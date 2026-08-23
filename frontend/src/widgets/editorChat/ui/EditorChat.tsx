import type { PointerEvent } from 'react'
import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { EditorChatMessage, EditorChatPosition } from '../model/types'
import { ChatCollapsedBar } from './components/ChatCollapsedBar'
import { ChatComposer } from './components/ChatComposer'
import { ChatHeader } from './components/ChatHeader'
import { ChatMessageList } from './components/ChatMessageList'
import styles from './EditorChat.module.css'

interface EditorChatProps {
  disabled?: boolean
  isPending?: boolean
  messages: EditorChatMessage[]
  onSendPrompt: (prompt: string) => void
}

const CHAT_WIDTH = 352
const CHAT_HEIGHT = 480
const VIEWPORT_GAP = 16

export function EditorChat({
  disabled = false,
  isPending = false,
  messages,
  onSendPrompt,
}: EditorChatProps) {
  const { t } = useTranslation()
  const dragOffset = useRef<EditorChatPosition | null>(null)
  const [draft, setDraft] = useState('')
  const [isExpanded, setIsExpanded] = useState(false)
  const [position, setPosition] = useState<EditorChatPosition | null>(null)

  function submitPrompt() {
    const prompt = draft.trim()

    if (!prompt || disabled || isPending) {
      return
    }

    setIsExpanded(true)
    setDraft('')
    onSendPrompt(prompt)
  }

  function handleHeaderPointerDown(event: PointerEvent<HTMLElement>) {
    if (event.button !== 0) {
      return
    }

    const panel = event.currentTarget.parentElement

    if (!panel) {
      return
    }

    const bounds = panel.getBoundingClientRect()
    dragOffset.current = {
      x: event.clientX - bounds.left,
      y: event.clientY - bounds.top,
    }
    event.currentTarget.setPointerCapture(event.pointerId)
  }

  function handleHeaderPointerMove(event: PointerEvent<HTMLElement>) {
    if (!dragOffset.current) {
      return
    }

    const maxX = Math.max(
      VIEWPORT_GAP,
      window.innerWidth - CHAT_WIDTH - VIEWPORT_GAP,
    )
    const maxY = Math.max(
      VIEWPORT_GAP,
      window.innerHeight - CHAT_HEIGHT - VIEWPORT_GAP,
    )
    setPosition({
      x: Math.min(
        Math.max(VIEWPORT_GAP, event.clientX - dragOffset.current.x),
        maxX,
      ),
      y: Math.min(
        Math.max(VIEWPORT_GAP, event.clientY - dragOffset.current.y),
        maxY,
      ),
    })
  }

  function handleHeaderPointerEnd(event: PointerEvent<HTMLElement>) {
    dragOffset.current = null

    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId)
    }
  }

  if (!isExpanded) {
    return (
      <aside className={styles.collapsed}>
        <ChatCollapsedBar
          disabled={disabled || isPending}
          onDraftChange={setDraft}
          onExpand={() => setIsExpanded(true)}
          onSubmit={submitPrompt}
          prompt={draft}
        />
      </aside>
    )
  }

  return (
    <aside
      className={styles.window}
      style={position ? { left: position.x, top: position.y } : undefined}
    >
      <ChatHeader
        onCollapse={() => setIsExpanded(false)}
        onPointerCancel={handleHeaderPointerEnd}
        onPointerDown={handleHeaderPointerDown}
        onPointerMove={handleHeaderPointerMove}
        onPointerUp={handleHeaderPointerEnd}
      />
      <ChatMessageList messages={messages} />
      <div className={styles.composer}>
        <ChatComposer
          disabled={disabled || isPending}
          onChange={setDraft}
          onSubmit={submitPrompt}
          placeholder={t('Ask AI to change the design…')}
          value={draft}
        />
      </div>
    </aside>
  )
}
