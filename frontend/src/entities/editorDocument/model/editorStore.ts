import { makeAutoObservable } from 'mobx'

import { removeNode } from './commands/removeNode'
import { setNodeVisibility } from './commands/setNodeVisibility'
import { toggleNodeVisibility } from './commands/toggleNodeVisibility'
import { findAncestorGroupIds, findNode } from './tree/findNode'
import type { EditorDocument, EditorNode, TextStyle } from './types'

const maximumTextLength = 1_000

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

    if (!nodeId) {
      return
    }

    for (const groupId of findAncestorGroupIds(this.document.layers, nodeId) ??
      []) {
      this.expandedGroupIds.add(groupId)
    }
  }

  setNodeVisibility(nodeId: string, isVisible: boolean) {
    setNodeVisibility(this.document.layers, nodeId, isVisible)
  }

  toggleNodeVisibility(nodeId: string) {
    toggleNodeVisibility(this.document.layers, nodeId)
  }

  deleteSelectedNode(): boolean {
    const selectedNode = this.selectedNode

    if (!selectedNode || selectedNode.isLocked) {
      return false
    }

    const wasDeleted = removeNode(this.document.layers, selectedNode.id)

    if (wasDeleted) {
      this.selectedNodeId = null
    }

    return wasDeleted
  }

  moveSelectedNode(offsetX: number, offsetY: number): boolean {
    const selectedNode = this.selectedNode

    if (
      !selectedNode ||
      selectedNode.isLocked ||
      !Number.isFinite(offsetX) ||
      !Number.isFinite(offsetY)
    ) {
      return false
    }

    if (selectedNode.type === 'shape' || selectedNode.type === 'group') {
      selectedNode.transform.x += offsetX
      selectedNode.transform.y += offsetY

      return true
    }

    if (selectedNode.type === 'text') {
      selectedNode.x += offsetX
      selectedNode.y += offsetY

      return true
    }

    return false
  }

  setShapeFillColor(nodeId: string, color: string) {
    const node = this.getEditableShape(nodeId)
    if (!node) {
      return
    }

    node.style.fill = { type: 'color', value: color }
  }

  setShapeFillOpacity(nodeId: string, opacity: number) {
    const node = this.getEditableShape(nodeId)

    if (!node || !Number.isFinite(opacity)) {
      return
    }

    node.style.fillOpacity = Math.min(Math.max(opacity, 0), 1)
  }

  setGradientStopColor(gradientId: string, stopIndex: number, color: string) {
    const gradient = this.document.definitions?.gradients.find(
      (item) => item.id === gradientId,
    )

    if (gradient?.type !== 'linearGradient') {
      return
    }

    if (
      gradient.stops.length !== 2 ||
      stopIndex < 0 ||
      stopIndex >= gradient.stops.length
    ) {
      return
    }

    gradient.stops[stopIndex].color = color
  }

  setShapeOpacity(nodeId: string, opacity: number) {
    const node = this.getEditableShape(nodeId)

    if (!node || !Number.isFinite(opacity)) {
      return
    }

    node.opacity = Math.min(Math.max(opacity, 0), 1)
  }

  setShapePosition(nodeId: string, x: number, y: number) {
    const node = this.getEditableShape(nodeId)

    if (!node || !Number.isFinite(x) || !Number.isFinite(y)) {
      return
    }

    node.transform.x = x
    node.transform.y = y
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

    node.transform.x = x
    node.transform.y = y
  }

  addShapeStroke(nodeId: string) {
    const node = this.getEditableShape(nodeId)

    if (!node) {
      return
    }

    node.style.stroke = {
      type: 'color',
      value:
        node.style.stroke.type === 'color'
          ? node.style.stroke.value
          : '#000000',
    }
    node.style.strokeWidth = Math.max(node.style.strokeWidth, 1)
  }

  setShapeStrokeColor(nodeId: string, color: string) {
    const node = this.getEditableShape(nodeId)

    if (!node) {
      return
    }

    node.style.stroke = { type: 'color', value: color }
    node.style.strokeWidth = Math.max(node.style.strokeWidth, 1)
  }

  setShapeStrokeWidth(nodeId: string, strokeWidth: number) {
    const node = this.getEditableShape(nodeId)

    if (!node || !Number.isFinite(strokeWidth)) {
      return
    }

    node.style.strokeWidth = Math.max(strokeWidth, 0)
  }

  removeShapeStroke(nodeId: string) {
    const node = this.getEditableShape(nodeId)

    if (!node) {
      return
    }

    node.style.stroke = { type: 'none' }
    node.style.strokeWidth = 0
  }

  setTextContent(nodeId: string, text: string) {
    const node = this.getEditableText(nodeId)

    if (node) {
      node.text = text.slice(0, maximumTextLength)
    }
  }

  setTextFillColor(nodeId: string, color: string) {
    const node = this.getEditableText(nodeId)

    if (node) {
      node.style.fill = { type: 'color', value: color }
    }
  }

  setTextFontFamily(nodeId: string, fontFamily: string) {
    const node = this.getEditableText(nodeId)

    if (node) {
      node.style.fontFamily = fontFamily
    }
  }

  setTextFontSize(nodeId: string, fontSize: number) {
    const node = this.getEditableText(nodeId)

    if (node && Number.isFinite(fontSize) && fontSize > 0) {
      node.style.fontSize = fontSize
    }
  }

  setTextFontStyle(
    nodeId: string,
    fontStyle: NonNullable<TextStyle['fontStyle']>,
  ) {
    const node = this.getEditableText(nodeId)

    if (node) {
      node.style.fontStyle = fontStyle
    }
  }

  setTextFontWeight(nodeId: string, fontWeight: number) {
    const node = this.getEditableText(nodeId)

    if (node && Number.isFinite(fontWeight) && fontWeight > 0) {
      node.style.fontWeight = fontWeight
    }
  }

  setTextAnchor(
    nodeId: string,
    textAnchor: NonNullable<TextStyle['textAnchor']>,
  ) {
    const node = this.getEditableText(nodeId)

    if (node) {
      node.style.textAnchor = textAnchor
    }
  }

  setTextPosition(nodeId: string, x: number, y: number) {
    const node = this.getEditableText(nodeId)

    if (node && Number.isFinite(x) && Number.isFinite(y)) {
      node.x = x
      node.y = y
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

  private getEditableText(nodeId: string) {
    const node = findNode(this.document.layers, nodeId)

    return node?.type === 'text' && !node.isLocked ? node : null
  }

  private getEditableShape(nodeId: string) {
    const node = findNode(this.document.layers, nodeId)

    return node?.type === 'shape' && !node.isLocked ? node : null
  }
}
