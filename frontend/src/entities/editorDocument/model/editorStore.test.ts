import { describe, expect, it } from 'vitest'

import { EditorStore } from './editorStore'
import type { EditorDocument } from './types'

const document: EditorDocument = {
  height: 100,
  layers: [
    {
      children: [
        {
          geometry: { cx: 50, cy: 50, r: 20, type: 'circle' },
          id: 'wheel',
          isLocked: false,
          isVisible: true,
          name: 'Wheel',
          opacity: 1,
          shapeType: 'circle',
          style: {
            fill: { type: 'color', value: '#000000' },
            fillOpacity: 1,
            stroke: { type: 'none' },
            strokeOpacity: 1,
            strokeWidth: 0,
          },
          transform: { rotation: 0, scaleX: 1, scaleY: 1, x: 0, y: 0 },
          type: 'shape',
        },
      ],
      id: 'taxi',
      isLocked: false,
      isVisible: true,
      name: 'Taxi',
      opacity: 1,
      transform: { rotation: 0, scaleX: 1, scaleY: 1, x: 0, y: 0 },
      type: 'group',
    },
    {
      id: 'title',
      isLocked: false,
      isVisible: true,
      name: 'Title',
      opacity: 1,
      style: {
        fill: { type: 'color', value: '#111111' },
        fontFamily: 'Inter',
        fontSize: 24,
        textAnchor: 'middle',
      },
      text: 'Initial title',
      transform: { rotation: 0, scaleX: 1, scaleY: 1, x: 0, y: 0 },
      type: 'text',
      x: 50,
      y: 20,
    },
  ],
  width: 100,
}

describe('EditorStore selection', () => {
  it('returns a group as the selected node', () => {
    const store = new EditorStore(document, 'taxi')

    expect(store.selectedNode).toMatchObject({ id: 'taxi', type: 'group' })
  })

  it('returns a nested shape as the selected node', () => {
    const store = new EditorStore(document)

    store.setSelectedNode('wheel')

    expect(store.selectedNode).toMatchObject({ id: 'wheel', type: 'shape' })
    expect(store.isGroupExpanded('taxi')).toBe(true)
  })

  it('toggles a group expansion state', () => {
    const store = new EditorStore(document)

    expect(store.isGroupExpanded('taxi')).toBe(false)

    store.toggleGroup('taxi')

    expect(store.isGroupExpanded('taxi')).toBe(true)

    store.toggleGroup('taxi')

    expect(store.isGroupExpanded('taxi')).toBe(false)
  })

  it('toggles visibility without changing a child node state', () => {
    const store = new EditorStore(document)

    store.toggleNodeVisibility('taxi')

    expect(store.selectedNode).toBeNull()
    expect(store.document.layers[0].isVisible).toBe(false)

    const group = store.document.layers[0]

    if (group.type !== 'group') {
      throw new Error('Expected the first test layer to be a group')
    }

    expect(group.children[0].isVisible).toBe(true)
  })

  it('deletes the selected unlocked node and clears the selection', () => {
    const store = new EditorStore(structuredClone(document), 'wheel')

    expect(store.deleteSelectedNode()).toBe(true)
    expect(store.selectedNode).toBeNull()

    const group = store.document.layers[0]

    if (group.type !== 'group') {
      throw new Error('Expected the first layer to be a group')
    }

    expect(group.children).toEqual([])
  })

  it('does not delete a locked selected node', () => {
    const store = new EditorStore(structuredClone(document), 'title')
    const selectedNode = store.selectedNode

    if (!selectedNode) {
      throw new Error('Expected a selected node')
    }

    selectedNode.isLocked = true

    expect(store.deleteSelectedNode()).toBe(false)
    expect(store.selectedNode).toMatchObject({ id: 'title' })
  })

  it('changes a shape fill color', () => {
    const store = new EditorStore(document)

    store.setShapeFillColor('wheel', '#FF0000')
    store.setSelectedNode('wheel')

    expect(store.selectedNode).toMatchObject({
      style: { fill: { type: 'color', value: '#FF0000' } },
    })
  })

  it('changes a shape fill opacity', () => {
    const store = new EditorStore(document)

    store.setShapeFillOpacity('wheel', 0.4)
    store.setSelectedNode('wheel')

    expect(store.selectedNode).toMatchObject({
      style: { fillOpacity: 0.4 },
    })
  })

  it('changes a shape position through its transform and common opacity', () => {
    const store = new EditorStore(document)

    store.setShapePosition('wheel', 12, -8)
    store.setShapeOpacity('wheel', 0.6)
    store.setSelectedNode('wheel')

    expect(store.selectedNode).toMatchObject({
      opacity: 0.6,
      transform: { x: 12, y: -8 },
    })
  })

  it('moves the selected editable node by the given offset', () => {
    const store = new EditorStore(structuredClone(document), 'wheel')

    expect(store.moveSelectedNode(-1, 1)).toBe(true)
    expect(store.selectedNode).toMatchObject({
      transform: { x: -1, y: 1 },
      type: 'shape',
    })

    store.setSelectedNode('taxi')

    expect(store.moveSelectedNode(1, -1)).toBe(true)
    expect(store.selectedNode).toMatchObject({
      transform: { x: 1, y: -1 },
      type: 'group',
    })

    store.setSelectedNode('title')

    expect(store.moveSelectedNode(1, -1)).toBe(true)
    expect(store.selectedNode).toMatchObject({ x: 51, y: 19, type: 'text' })
  })

  it('does not move a missing or locked selection', () => {
    const store = new EditorStore(structuredClone(document))

    expect(store.moveSelectedNode(1, 1)).toBe(false)

    store.setSelectedNode('wheel')
    const selectedNode = store.selectedNode

    if (!selectedNode) {
      throw new Error('Expected a selected node')
    }

    selectedNode.isLocked = true

    expect(store.moveSelectedNode(1, 1)).toBe(false)
  })

  it('changes editable text properties', () => {
    const store = new EditorStore(document)

    store.setTextContent('title', 'Updated title')
    store.setTextFillColor('title', '#6C3BFF')
    store.setTextFontFamily('title', 'Roboto')
    store.setTextFontSize('title', 36)
    store.setTextFontStyle('title', 'italic')
    store.setTextFontWeight('title', 700)
    store.setTextAnchor('title', 'start')
    store.setTextPosition('title', 30, 40)
    store.setSelectedNode('title')

    expect(store.selectedNode).toMatchObject({
      style: {
        fill: { type: 'color', value: '#6C3BFF' },
        fontFamily: 'Roboto',
        fontSize: 36,
        fontStyle: 'italic',
        fontWeight: 700,
        textAnchor: 'start',
      },
      text: 'Updated title',
      type: 'text',
      x: 30,
      y: 40,
    })
  })
})
