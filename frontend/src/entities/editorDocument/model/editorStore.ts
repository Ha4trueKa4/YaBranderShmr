import { makeAutoObservable } from 'mobx'

import { removeNode } from './commands/removeNode'
import {
  areDocumentsEqual,
  createDocumentSnapshot,
  editorHistoryLimit,
} from './editorHistory/documentSnapshot'
import {
  commitHistory,
  createHistory,
  type EditorHistory,
  redoHistory,
  undoHistory,
} from './editorHistory/history'
import { createSvgNodeEditing, type SvgNodeEditing } from './svgNodeEditing'
import { findAncestorGroupIds, findNode } from './tree/findNode'
import type { EditorDocument, EditorNode } from './types'

/**
 * Состояние взаимодействия с редактором.
 * История хранит только документ. Выделение и состояние интерфейса не являются
 * частью SVG и поэтому не восстанавливаются при undo/redo.
 */
export class EditorStore {
  document: EditorDocument
  history: EditorHistory<EditorDocument>
  expandedGroupIds: Set<string>
  selectedNodeId: string | null
  selectedNodeIds: Set<string>
  svgNodeEditing: SvgNodeEditing
  zoom = 1
  private transactionInitialDocument: EditorDocument | null = null

  constructor(
    document: EditorDocument,
    selectedNodeId: string | null = null,
    expandedGroupIds: string[] = [],
  ) {
    this.document = createDocumentSnapshot(document)
    this.history = createHistory(createDocumentSnapshot(this.document))
    this.expandedGroupIds = new Set(expandedGroupIds)
    this.selectedNodeId = selectedNodeId
    this.selectedNodeIds = new Set(selectedNodeId ? [selectedNodeId] : [])
    this.svgNodeEditing = createSvgNodeEditing({
      applyDocumentChange: (change) => this.applyDocumentChange(change),
      getDocument: () => this.document,
    })

    makeAutoObservable(this, { svgNodeEditing: false }, { autoBind: true })
  }

  get selectedNode(): EditorNode | null {
    return findNode(this.document.layers, this.selectedNodeId)
  }

  get selectedNodes(): EditorNode[] {
    return [...this.selectedNodeIds]
      .map((nodeId) => findNode(this.document.layers, nodeId))
      .filter((node): node is EditorNode => node !== null)
  }

  get canUndo(): boolean {
    return this.history.past.length > 0
  }

  get canRedo(): boolean {
    return this.history.future.length > 0
  }

  beginHistoryTransaction() {
    if (!this.transactionInitialDocument) {
      this.transactionInitialDocument = createDocumentSnapshot(this.document)
    }
  }

  commitHistoryTransaction(): boolean {
    const initialDocument = this.transactionInitialDocument

    if (!initialDocument) {
      return false
    }

    this.transactionInitialDocument = null

    return this.commitDocumentChange(initialDocument)
  }

  cancelHistoryTransaction(): boolean {
    const initialDocument = this.transactionInitialDocument

    if (!initialDocument) {
      return false
    }

    this.transactionInitialDocument = null

    if (areDocumentsEqual(initialDocument, this.document)) {
      return false
    }

    this.document = createDocumentSnapshot(initialDocument)
    this.removeMissingSelectedNodes()

    return true
  }

  undo(): boolean {
    this.commitHistoryTransaction()
    const nextHistory = undoHistory(this.history)

    if (!nextHistory) {
      return false
    }

    this.history = nextHistory
    this.document = createDocumentSnapshot(nextHistory.present)
    this.removeMissingSelectedNodes()

    return true
  }

  redo(): boolean {
    this.commitHistoryTransaction()
    const nextHistory = redoHistory(this.history, editorHistoryLimit)

    if (!nextHistory) {
      return false
    }

    this.history = nextHistory
    this.document = createDocumentSnapshot(nextHistory.present)
    this.removeMissingSelectedNodes()

    return true
  }

  setSelectedNode(nodeId: string | null) {
    this.selectedNodeId = nodeId
    this.selectedNodeIds.clear()

    if (nodeId) {
      this.selectedNodeIds.add(nodeId)
    }

    if (!nodeId) {
      return
    }

    for (const groupId of findAncestorGroupIds(this.document.layers, nodeId) ??
      []) {
      this.expandedGroupIds.add(groupId)
    }
  }

  selectNode(nodeId: string, isMultiSelect = false) {
    const node = findNode(this.document.layers, nodeId)

    if (!isMultiSelect || node?.type === 'group' || node?.type === 'raw') {
      // A group owns its children in the selection, so it replaces any
      // existing multi-selection rather than being added to it.
      this.setSelectedNode(nodeId)

      return
    }

    if (
      this.selectedNodes.some((selectedNode) => selectedNode.type === 'group')
    ) {
      this.setSelectedNode(nodeId)

      return
    }

    if (this.selectedNodeIds.has(nodeId)) {
      this.selectedNodeIds.delete(nodeId)
      this.selectedNodeId = [...this.selectedNodeIds].at(-1) ?? null

      return
    }

    this.selectedNodeIds.add(nodeId)
    this.selectedNodeId = nodeId
  }

  deleteSelectedNode(): boolean {
    const selectedNode = this.selectedNode

    if (!selectedNode || selectedNode.isLocked) {
      return false
    }

    const wasDeleted = this.applyDocumentChange(() =>
      removeNode(this.document.layers, selectedNode.id),
    )

    if (wasDeleted) {
      this.setSelectedNode(null)
    }

    return wasDeleted
  }

  moveSelectedNode(offsetX: number, offsetY: number): boolean {
    const selectedNodes = this.selectedNodes

    if (
      selectedNodes.length === 0 ||
      selectedNodes.some((node) => node.isLocked && node.type !== 'raw') ||
      selectedNodes.some(
        (node) =>
          node.type !== 'shape' &&
          node.type !== 'group' &&
          node.type !== 'text' &&
          node.type !== 'raw',
      ) ||
      !Number.isFinite(offsetX) ||
      !Number.isFinite(offsetY)
    ) {
      return false
    }

    return this.applyDocumentChange(() => {
      for (const selectedNode of selectedNodes) {
        if (
          selectedNode.type === 'shape' ||
          selectedNode.type === 'group' ||
          selectedNode.type === 'raw'
        ) {
          selectedNode.transform.x += offsetX
          selectedNode.transform.y += offsetY
        } else if (selectedNode.type === 'text') {
          selectedNode.x += offsetX
          selectedNode.y += offsetY
        }
      }
    })
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

  private applyDocumentChange(change: () => void): boolean {
    const initialDocument = this.transactionInitialDocument
      ? null
      : createDocumentSnapshot(this.document)

    change()

    return initialDocument ? this.commitDocumentChange(initialDocument) : true
  }

  private commitDocumentChange(initialDocument: EditorDocument): boolean {
    const nextDocument = createDocumentSnapshot(this.document)

    if (areDocumentsEqual(initialDocument, nextDocument)) {
      return false
    }

    this.history = commitHistory(this.history, nextDocument, editorHistoryLimit)

    return true
  }

  private removeMissingSelectedNodes() {
    const existingNodeIds = [...this.selectedNodeIds].filter((nodeId) =>
      findNode(this.document.layers, nodeId),
    )

    this.selectedNodeIds = new Set(existingNodeIds)
    this.selectedNodeId = existingNodeIds.at(-1) ?? null
  }
}
