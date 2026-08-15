import type { EditorNode } from '../types'

export function findNode(
  nodes: EditorNode[],
  nodeId: string | null,
): EditorNode | null {
  if (!nodeId) {
    return null
  }

  for (const node of nodes) {
    if (node.id === nodeId) {
      return node
    }

    if (node.type === 'group') {
      const childNode = findNode(node.children, nodeId)

      if (childNode) {
        return childNode
      }
    }
  }

  return null
}

export function findAncestorGroupIds(
  nodes: EditorNode[],
  nodeId: string,
): string[] | null {
  for (const node of nodes) {
    if (node.id === nodeId) {
      return []
    }

    if (node.type === 'group') {
      const ancestorGroupIds = findAncestorGroupIds(node.children, nodeId)

      if (ancestorGroupIds) {
        return [node.id, ...ancestorGroupIds]
      }
    }
  }

  return null
}
