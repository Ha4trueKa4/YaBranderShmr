import { findNode } from '../tree/findNode'
import type { EditorNode } from '../types'

export function setNodeVisibility(
  nodes: EditorNode[],
  nodeId: string,
  isVisible: boolean,
) {
  const node = findNode(nodes, nodeId)

  if (node) {
    node.isVisible = isVisible
  }
}
