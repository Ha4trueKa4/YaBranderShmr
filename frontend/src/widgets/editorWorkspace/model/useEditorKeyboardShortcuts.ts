import { useEffect } from 'react'

interface UseEditorKeyboardShortcutsParams {
  canEdit: boolean
  onDeleteSelectedNode: () => boolean
  onMoveSelectedNode: (offsetX: number, offsetY: number) => boolean
  onRedo: () => boolean
  onUndo: () => boolean
}

const arrowKeyOffsets = {
  ArrowDown: { x: 0, y: 1 },
  ArrowLeft: { x: -1, y: 0 },
  ArrowRight: { x: 1, y: 0 },
  ArrowUp: { x: 0, y: -1 },
}

function isEditableElement(target: EventTarget | null) {
  return (
    target instanceof HTMLInputElement ||
    target instanceof HTMLSelectElement ||
    target instanceof HTMLTextAreaElement ||
    (target instanceof HTMLElement && target.isContentEditable)
  )
}

export function useEditorKeyboardShortcuts({
  canEdit,
  onDeleteSelectedNode,
  onMoveSelectedNode,
  onRedo,
  onUndo,
}: UseEditorKeyboardShortcutsParams) {
  useEffect(() => {
    if (!canEdit) {
      return
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (
        event.defaultPrevented ||
        event.altKey ||
        isEditableElement(event.target)
      ) {
        return
      }

      const isUndoShortcut =
        (event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'z'

      if (isUndoShortcut) {
        const wasHandled = event.shiftKey ? onRedo() : onUndo()

        if (wasHandled) {
          event.preventDefault()
        }

        return
      }

      if (event.metaKey || event.ctrlKey) {
        return
      }

      const offset = arrowKeyOffsets[event.key as keyof typeof arrowKeyOffsets]

      if (offset && onMoveSelectedNode(offset.x, offset.y)) {
        event.preventDefault()

        return
      }

      if (event.key === 'Backspace' && onDeleteSelectedNode()) {
        event.preventDefault()
      }
    }

    window.addEventListener('keydown', handleKeyDown)

    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [canEdit, onDeleteSelectedNode, onMoveSelectedNode, onRedo, onUndo])
}
