import { makeAutoObservable } from 'mobx'

import type { EditorDocument, EditorElement, EditorNode } from './types'

/**
 * Состояние взаимодействия с редактором.
 * Undo/redo и операции редактирования будут добавлены отдельными feature.
 */
export class EditorStore {
  document: EditorDocument
  expandedGroupIds: Set<string>
  selectedLayerId: string | null
  zoom = 1

  constructor(
    document: EditorDocument,
    selectedLayerId: string | null = null,
    expandedGroupIds: string[] = [],
  ) {
    this.document = document
    this.expandedGroupIds = new Set(expandedGroupIds)
    this.selectedLayerId = selectedLayerId

    makeAutoObservable(this, {}, { autoBind: true })
  }

  get selectedLayer(): EditorElement | null {
    return findEditorElement(this.document.layers, this.selectedLayerId)
  }

  setSelectedLayer(layerId: string | null) {
    this.selectedLayerId = layerId
  }

  toggleGroup(groupId: string) {
    if (this.expandedGroupIds.has(groupId)) {
      this.expandedGroupIds.delete(groupId)

      return
    }

    this.expandedGroupIds.add(groupId)
  }
}

function findEditorElement(
  nodes: EditorNode[],
  layerId: string | null,
): EditorElement | null {
  if (!layerId) {
    return null
  }

  for (const node of nodes) {
    if (node.type === 'group') {
      const layer = findEditorElement(node.children, layerId)

      if (layer) {
        return layer
      }

      continue
    }

    if (node.id === layerId) {
      return node
    }
  }

  return null
}
