import { findNode } from '../tree/findNode'
import type { EditorDocument } from '../types'

export function getEditableShape(document: EditorDocument, nodeId: string) {
  const node = findNode(document.layers, nodeId)

  return node?.type === 'shape' && !node.isLocked ? node : null
}

export function getEditableText(document: EditorDocument, nodeId: string) {
  const node = findNode(document.layers, nodeId)

  return node?.type === 'text' && !node.isLocked ? node : null
}
