import type { EditorNode } from '../types'

export function removeNode(nodes: EditorNode[], nodeId: string): boolean {
  const nodeIndex = nodes.findIndex((node) => node.id === nodeId)

  if (nodeIndex >= 0) {
    nodes.splice(nodeIndex, 1)

    return true
  }

  return nodes.some(
    (node) => node.type === 'group' && removeNode(node.children, nodeId),
  )
}
