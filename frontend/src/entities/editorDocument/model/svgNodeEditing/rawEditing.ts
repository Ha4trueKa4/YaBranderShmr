import { findNode } from '../tree/findNode'
import type { SvgEditingContext } from './editingContext'

export function createRawEditing({
  applyDocumentChange,
  getDocument,
}: SvgEditingContext) {
  return {
    setRawPosition(nodeId: string, x: number, y: number) {
      const node = findNode(getDocument().layers, nodeId)

      if (node?.type !== 'raw' || !Number.isFinite(x) || !Number.isFinite(y)) {
        return
      }

      applyDocumentChange(() => {
        node.transform.x = x
        node.transform.y = y
      })
    },
  }
}
