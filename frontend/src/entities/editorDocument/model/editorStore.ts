import { makeAutoObservable } from 'mobx'

import { setNodeVisibility } from './commands/setNodeVisibility'
import { toggleNodeVisibility } from './commands/toggleNodeVisibility'
import { findNode } from './tree/findNode'
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
    return findNode(this.document.layers, this.selectedNodeId)
  }

  setSelectedNode(nodeId: string | null) {
    this.selectedNodeId = nodeId
  }

  setNodeVisibility(nodeId: string, isVisible: boolean) {
    setNodeVisibility(this.document.layers, nodeId, isVisible)
  }

  toggleNodeVisibility(nodeId: string) {
    toggleNodeVisibility(this.document.layers, nodeId)
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
