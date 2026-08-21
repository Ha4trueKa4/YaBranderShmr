import { makeAutoObservable } from 'mobx'

import { removeNode } from './commands/removeNode'
import { setNodeVisibility } from './commands/setNodeVisibility'
import { toggleNodeVisibility } from './commands/toggleNodeVisibility'
import {
  areDocumentsEqual,
  createDocumentSnapshot,
  editorHistoryLimit,
} from './documentSnapshot'
import {
  commitHistory,
  createHistory,
  type EditorHistory,
  redoHistory,
  undoHistory,
} from './history'
import { findAncestorGroupIds, findNode } from './tree/findNode'
import type { EditorDocument, EditorNode, TextStyle } from './types'

const maximumTextLength = 1_000
const hexColorPattern = /^#(?:[\dA-Fa-f]{3}|[\dA-Fa-f]{6})$/

function isValidHexColor(color: string) {
  return hexColorPattern.test(color)
}

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

    makeAutoObservable(this, {}, { autoBind: true })
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

  setNodeVisibility(nodeId: string, isVisible: boolean) {
    this.applyDocumentChange(() =>
      setNodeVisibility(this.document.layers, nodeId, isVisible),
    )
  }

  toggleNodeVisibility(nodeId: string) {
    this.applyDocumentChange(() =>
      toggleNodeVisibility(this.document.layers, nodeId),
    )
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
      selectedNodes.some((node) => node.isLocked) ||
      selectedNodes.some(
        (node) =>
          node.type !== 'shape' &&
          node.type !== 'group' &&
          node.type !== 'text',
      ) ||
      !Number.isFinite(offsetX) ||
      !Number.isFinite(offsetY)
    ) {
      return false
    }

    return this.applyDocumentChange(() => {
      for (const selectedNode of selectedNodes) {
        if (selectedNode.type === 'shape' || selectedNode.type === 'group') {
          selectedNode.transform.x += offsetX
          selectedNode.transform.y += offsetY
        } else if (selectedNode.type === 'text') {
          selectedNode.x += offsetX
          selectedNode.y += offsetY
        }
      }
    })
  }

  setShapeFillColor(nodeId: string, color: string) {
    const node = this.getEditableShape(nodeId)
    if (!node) {
      return
    }

    this.applyDocumentChange(() => {
      node.style.fill = { type: 'color', value: color }
    })
  }

  setShapeFillOpacity(nodeId: string, opacity: number) {
    const node = this.getEditableShape(nodeId)

    if (!node || !Number.isFinite(opacity)) {
      return
    }

    this.applyDocumentChange(() => {
      node.style.fillOpacity = Math.min(Math.max(opacity, 0), 1)
    })
  }

  setGradientStopColor(gradientId: string, stopIndex: number, color: string) {
    const gradient = this.document.definitions?.gradients.find(
      (item) => item.id === gradientId,
    )

    if (!gradient) {
      return
    }

    if (
      !Number.isInteger(stopIndex) ||
      stopIndex < 0 ||
      stopIndex >= gradient.stops.length ||
      !isValidHexColor(color)
    ) {
      return
    }

    this.applyDocumentChange(() => {
      gradient.stops[stopIndex].color = color
    })
  }

  setShapeOpacity(nodeId: string, opacity: number) {
    const node = this.getEditableShape(nodeId)

    if (!node || !Number.isFinite(opacity)) {
      return
    }

    this.applyDocumentChange(() => {
      node.opacity = Math.min(Math.max(opacity, 0), 1)
    })
  }

  setShapePosition(nodeId: string, x: number, y: number) {
    const node = this.getEditableShape(nodeId)

    if (!node || !Number.isFinite(x) || !Number.isFinite(y)) {
      return
    }

    this.applyDocumentChange(() => {
      node.transform.x = x
      node.transform.y = y
    })
  }

  setGroupPosition(nodeId: string, x: number, y: number) {
    const node = findNode(this.document.layers, nodeId)

    if (
      node?.type !== 'group' ||
      node.isLocked ||
      !Number.isFinite(x) ||
      !Number.isFinite(y)
    ) {
      return
    }

    this.applyDocumentChange(() => {
      node.transform.x = x
      node.transform.y = y
    })
  }

  addShapeStroke(nodeId: string) {
    const node = this.getEditableShape(nodeId)

    if (!node) {
      return
    }

    const currentStrokeColor =
      node.style.stroke.type === 'color' ? node.style.stroke.value : null
    const currentStrokeWidth = node.style.strokeWidth

    if (
      (currentStrokeColor !== null && !isValidHexColor(currentStrokeColor)) ||
      !Number.isFinite(currentStrokeWidth) ||
      currentStrokeWidth < 0
    ) {
      return
    }

    this.applyDocumentChange(() => {
      node.style.stroke = {
        type: 'color',
        value: currentStrokeColor ?? '#000000',
      }
      node.style.strokeWidth = Math.max(node.style.strokeWidth, 1)
    })
  }

  setShapeStrokeColor(nodeId: string, color: string) {
    const node = this.getEditableShape(nodeId)

    if (!node || !isValidHexColor(color)) {
      return
    }

    this.applyDocumentChange(() => {
      node.style.stroke = { type: 'color', value: color }
      node.style.strokeWidth = Math.max(node.style.strokeWidth, 1)
    })
  }

  setShapeStrokeWidth(nodeId: string, strokeWidth: number) {
    const node = this.getEditableShape(nodeId)

    if (!node || !Number.isFinite(strokeWidth) || strokeWidth < 0) {
      return
    }

    this.applyDocumentChange(() => {
      node.style.strokeWidth = strokeWidth
    })
  }

  removeShapeStroke(nodeId: string) {
    const node = this.getEditableShape(nodeId)

    if (!node) {
      return
    }

    this.applyDocumentChange(() => {
      node.style.stroke = { type: 'none' }
      node.style.strokeWidth = 0
    })
  }

  setTextContent(nodeId: string, text: string) {
    const node = this.getEditableText(nodeId)

    if (node) {
      this.applyDocumentChange(() => {
        node.text = text.slice(0, maximumTextLength)
      })
    }
  }

  setTextFillColor(nodeId: string, color: string) {
    const node = this.getEditableText(nodeId)

    if (node) {
      this.applyDocumentChange(() => {
        node.style.fill = { type: 'color', value: color }
      })
    }
  }

  setTextFontFamily(nodeId: string, fontFamily: string) {
    const node = this.getEditableText(nodeId)

    if (node) {
      this.applyDocumentChange(() => {
        node.style.fontFamily = fontFamily
      })
    }
  }

  setTextFontSize(nodeId: string, fontSize: number) {
    const node = this.getEditableText(nodeId)

    if (node && Number.isFinite(fontSize) && fontSize > 0) {
      this.applyDocumentChange(() => {
        node.style.fontSize = fontSize
      })
    }
  }

  setTextFontStyle(
    nodeId: string,
    fontStyle: NonNullable<TextStyle['fontStyle']>,
  ) {
    const node = this.getEditableText(nodeId)

    if (node) {
      this.applyDocumentChange(() => {
        node.style.fontStyle = fontStyle
      })
    }
  }

  setTextFontWeight(nodeId: string, fontWeight: number) {
    const node = this.getEditableText(nodeId)

    if (node && Number.isFinite(fontWeight) && fontWeight > 0) {
      this.applyDocumentChange(() => {
        node.style.fontWeight = fontWeight
      })
    }
  }

  setTextAnchor(
    nodeId: string,
    textAnchor: NonNullable<TextStyle['textAnchor']>,
  ) {
    const node = this.getEditableText(nodeId)

    if (node) {
      this.applyDocumentChange(() => {
        node.style.textAnchor = textAnchor
      })
    }
  }

  setTextPosition(nodeId: string, x: number, y: number) {
    const node = this.getEditableText(nodeId)

    if (node && Number.isFinite(x) && Number.isFinite(y)) {
      this.applyDocumentChange(() => {
        node.x = x
        node.y = y
      })
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

  private getEditableText(nodeId: string) {
    const node = findNode(this.document.layers, nodeId)

    return node?.type === 'text' && !node.isLocked ? node : null
  }

  private getEditableShape(nodeId: string) {
    const node = findNode(this.document.layers, nodeId)

    return node?.type === 'shape' && !node.isLocked ? node : null
  }
}
