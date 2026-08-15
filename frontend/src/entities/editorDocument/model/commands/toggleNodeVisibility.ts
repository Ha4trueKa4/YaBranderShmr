import { findNode } from '../tree/findNode'
import type { EditorNode } from '../types'
import { setNodeVisibility } from './setNodeVisibility'

export function toggleNodeVisibility(nodes: EditorNode[], nodeId: string) {
  const node = findNode(nodes, nodeId)

  if (node) {
    setNodeVisibility(nodes, nodeId, !node.isVisible)
  }
}
