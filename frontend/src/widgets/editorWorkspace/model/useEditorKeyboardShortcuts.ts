import { useEffect, useRef } from 'react'

interface UseEditorKeyboardShortcutsParams {
  canEdit: boolean
  onDeleteSelectedNode: () => boolean
  onMoveEnd: () => void
  onMoveStart: () => void
  onMoveSelectedNode: (offsetX: number, offsetY: number) => boolean
  onRedo: () => boolean
  onSaveVersion: () => void
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
  onMoveEnd,
  onMoveStart,
  onMoveSelectedNode,
  onRedo,
  onSaveVersion,
  onUndo,
}: UseEditorKeyboardShortcutsParams) {
  const pressedArrowKeysReference = useRef(new Set<string>())

  useEffect(() => {
    if (!canEdit) {
      return
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.defaultPrevented || event.altKey) {
        return
      }

      const isSaveShortcut =
        (event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 's'

      if (isSaveShortcut) {
        event.preventDefault()

        if (!event.repeat) {
          onSaveVersion()
        }

        return
      }

      if (isEditableElement(event.target)) {
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

      if (offset) {
        const pressedArrowKeys = pressedArrowKeysReference.current
        const isNewArrowKey = !pressedArrowKeys.has(event.key)

        if (isNewArrowKey) {
          onMoveStart()
        }

        if (onMoveSelectedNode(offset.x, offset.y)) {
          pressedArrowKeys.add(event.key)
          event.preventDefault()

          return
        }

        if (isNewArrowKey && pressedArrowKeys.size === 0) {
          onMoveEnd()
        }

        return
      }

      if (event.key === 'Backspace' && onDeleteSelectedNode()) {
        event.preventDefault()
      }
    }

    function finishKeyboardMove() {
      const pressedArrowKeys = pressedArrowKeysReference.current

      if (pressedArrowKeys.size === 0) {
        return
      }

      pressedArrowKeys.clear()
      onMoveEnd()
    }

    function handleKeyUp(event: KeyboardEvent) {
      if (!pressedArrowKeysReference.current.delete(event.key)) {
        return
      }

      if (pressedArrowKeysReference.current.size === 0) {
        onMoveEnd()
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    window.addEventListener('keyup', handleKeyUp)
    window.addEventListener('blur', finishKeyboardMove)

    return () => {
      finishKeyboardMove()
      window.removeEventListener('keydown', handleKeyDown)
      window.removeEventListener('keyup', handleKeyUp)
      window.removeEventListener('blur', finishKeyboardMove)
    }
  }, [
    canEdit,
    onDeleteSelectedNode,
    onMoveEnd,
    onMoveSelectedNode,
    onMoveStart,
    onRedo,
    onSaveVersion,
    onUndo,
  ])
}
