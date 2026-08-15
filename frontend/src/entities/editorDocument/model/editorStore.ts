import { makeAutoObservable } from 'mobx'

import type { EditorDocument, EditorNode } from './types'

/**
 * Состояние взаимодействия с редактором.
 * Undo/redo и операции редактирования будут добавлены отдельными feature.
 */
export class EditorStore {
  document: EditorDocument
  expandedGroupIds: Set<string>
  selectedNodeId: string | null
  zoom = 1

  constructor(
    document: EditorDocument,
    selectedNodeId: string | null = null,
    expandedGroupIds: string[] = [],
  ) {
    this.document = document
    this.expandedGroupIds = new Set(expandedGroupIds)
    this.selectedNodeId = selectedNodeId

    makeAutoObservable(this, {}, { autoBind: true })
  }

  get selectedNode(): EditorNode | null {
    return findEditorNode(this.document.layers, this.selectedNodeId)
  }

  setSelectedNode(nodeId: string | null) {
    this.selectedNodeId = nodeId
  }

  setNodeVisibility(nodeId: string, isVisible: boolean) {
    const node = findEditorNode(this.document.layers, nodeId)

    if (node) {
      node.isVisible = isVisible
    }
  }

  toggleNodeVisibility(nodeId: string) {
    const node = findEditorNode(this.document.layers, nodeId)

    if (node) {
      this.setNodeVisibility(nodeId, !node.isVisible)
    }
  }

  isGroupExpanded(groupId: string): boolean {
    return this.expandedGroupIds.has(groupId)
  }

  toggleGroup(groupId: string) {
    if (this.expandedGroupIds.has(groupId)) {
      this.expandedGroupIds.delete(groupId)

      return
    }

    this.expandedGroupIds.add(groupId)
  }
}

function findEditorNode(
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
      const childNode = findEditorNode(node.children, nodeId)

      if (childNode) {
        return childNode
      }
    }
  }

  return null
}
