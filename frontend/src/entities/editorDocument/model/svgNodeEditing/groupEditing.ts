import { findNode } from '../tree/findNode'
import type { SvgEditingContext } from './editingContext'

export function createGroupEditing({
  applyDocumentChange,
  getDocument,
}: SvgEditingContext) {
  return {
    setGroupPosition(nodeId: string, x: number, y: number) {
      const node = findNode(getDocument().layers, nodeId)

      if (
        node?.type !== 'group' ||
        node.isLocked ||
        !Number.isFinite(x) ||
        !Number.isFinite(y)
      ) {
        return
      }

      applyDocumentChange(() => {
        node.transform.x = x
        node.transform.y = y
      })
    },
  }
}
