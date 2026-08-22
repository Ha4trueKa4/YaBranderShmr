import { describe, expect, it } from 'vitest'

import { EditorStore } from './editorStore'
import type { EditorDocument } from './types'

const document: EditorDocument = {
  definitions: {
    gradients: [
      {
        attributes: { x1: '0%', x2: '100%', y1: '0%', y2: '0%' },
        id: 'wheel-gradient',
        stops: [
          { color: '#FF0000', offset: '0%' },
          { color: '#0000FF', offset: '100%' },
        ],
        type: 'linearGradient',
      },
      {
        attributes: { cx: '50%', cy: '50%', r: '50%' },
        id: 'shadow-gradient',
        stops: [
          { color: '#FFFFFF', offset: '0%' },
          { color: '#808080', offset: '50%' },
          { color: '#000000', offset: '100%' },
        ],
        type: 'radialGradient',
      },
    ],
  },
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
  it('undoes and redoes a document change', () => {
    const store = new EditorStore(structuredClone(document))

    store.svgNodeEditing.setShapeFillColor('wheel', '#FF0000')

    expect(store.canUndo).toBe(true)
    expect(store.undo()).toBe(true)
    expect(store.selectedNode).toBeNull()
    expect(store.document.layers[0]).toMatchObject({
      children: [{ style: { fill: { value: '#000000' } } }],
    })
    expect(store.canRedo).toBe(true)

    expect(store.redo()).toBe(true)
    expect(store.document.layers[0]).toMatchObject({
      children: [{ style: { fill: { value: '#FF0000' } } }],
    })
  })

  it('records a history transaction as one change', () => {
    const store = new EditorStore(structuredClone(document))

    store.beginHistoryTransaction()
    store.svgNodeEditing.setShapePosition('wheel', 10, 10)
    store.svgNodeEditing.setShapePosition('wheel', 20, 20)

    expect(store.commitHistoryTransaction()).toBe(true)
    expect(store.undo()).toBe(true)
    expect(store.document.layers[0]).toMatchObject({
      children: [{ transform: { x: 0, y: 0 } }],
    })
    expect(store.canUndo).toBe(false)
  })

  it('restores the initial document when a history transaction is cancelled', () => {
    const store = new EditorStore(structuredClone(document))

    store.beginHistoryTransaction()
    store.svgNodeEditing.setShapePosition('wheel', 20, 20)

    expect(store.cancelHistoryTransaction()).toBe(true)
    expect(store.document.layers[0]).toMatchObject({
      children: [{ transform: { x: 0, y: 0 } }],
    })
    expect(store.canUndo).toBe(false)
  })

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

  it('adds and removes editable nodes from the selection', () => {
    const store = new EditorStore(structuredClone(document))

    store.selectNode('wheel')
    store.selectNode('title', true)

    expect([...store.selectedNodeIds]).toEqual(['wheel', 'title'])
    expect(store.selectedNode).toMatchObject({ id: 'title' })

    store.selectNode('wheel', true)

    expect([...store.selectedNodeIds]).toEqual(['title'])
  })

  it('replaces a multi-selection when a group is selected', () => {
    const store = new EditorStore(structuredClone(document))

    store.selectNode('wheel')
    store.selectNode('title', true)
    store.selectNode('taxi', true)

    expect([...store.selectedNodeIds]).toEqual(['taxi'])
    expect(store.selectedNode).toMatchObject({ id: 'taxi', type: 'group' })
  })

  it('toggles visibility without changing a child node state', () => {
    const store = new EditorStore(document)

    store.svgNodeEditing.toggleNodeVisibility('taxi')

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

    store.svgNodeEditing.setShapeFillColor('wheel', '#FF0000')
    store.setSelectedNode('wheel')

    expect(store.selectedNode).toMatchObject({
      style: { fill: { type: 'color', value: '#FF0000' } },
    })
  })

  it('changes a shape fill opacity', () => {
    const store = new EditorStore(document)

    store.svgNodeEditing.setShapeFillOpacity('wheel', 0.4)
    store.setSelectedNode('wheel')

    expect(store.selectedNode).toMatchObject({
      style: { fillOpacity: 0.4 },
    })
  })

  it('changes colors of every stop in linear and radial gradients', () => {
    const store = new EditorStore(structuredClone(document))

    store.svgNodeEditing.setGradientStopColor('wheel-gradient', 0, '#FFD400')
    store.svgNodeEditing.setGradientStopColor('wheel-gradient', 1, '#6C3BFF')
    store.svgNodeEditing.setGradientStopColor('shadow-gradient', 1, '#FF00FF')

    expect(store.document.definitions?.gradients[0]?.stops).toEqual([
      { color: '#FFD400', offset: '0%' },
      { color: '#6C3BFF', offset: '100%' },
    ])
    expect(store.document.definitions?.gradients[1]?.stops[1]).toEqual({
      color: '#FF00FF',
      offset: '50%',
    })
  })

  it('changes a shape position through its transform and common opacity', () => {
    const store = new EditorStore(document)

    store.svgNodeEditing.setShapePosition('wheel', 12, -8)
    store.svgNodeEditing.setShapeOpacity('wheel', 0.6)
    store.setSelectedNode('wheel')

    expect(store.selectedNode).toMatchObject({
      opacity: 0.6,
      transform: { x: 12, y: -8 },
    })
  })

  it('changes a group position through its transform', () => {
    const store = new EditorStore(structuredClone(document))

    store.svgNodeEditing.setGroupPosition('taxi', 12, -8)
    store.setSelectedNode('taxi')

    expect(store.selectedNode).toMatchObject({
      transform: { x: 12, y: -8 },
      type: 'group',
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

  it('moves all selected editable nodes by the given offset', () => {
    const store = new EditorStore(structuredClone(document))

    store.selectNode('wheel')
    store.selectNode('title', true)

    expect(store.moveSelectedNode(4, -2)).toBe(true)
    expect(store.selectedNodes).toMatchObject([
      { id: 'wheel', transform: { x: 4, y: -2 } },
      { id: 'title', x: 54, y: 18 },
    ])
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

  it('moves a selected raw node by the given offset', () => {
    const documentWithRawNode = structuredClone(document)
    documentWithRawNode.layers.push({
      id: 'unsupported',
      isLocked: false,
      isVisible: true,
      name: 'Unsupported',
      opacity: 1,
      rawSvg: '<foreignObject />',
      transform: { rotation: 0, scaleX: 1, scaleY: 1, x: 0, y: 0 },
      type: 'raw',
    })
    const store = new EditorStore(documentWithRawNode, 'unsupported')

    expect(store.moveSelectedNode(1, 1)).toBe(true)
    expect(store.selectedNode).toMatchObject({
      transform: { x: 1, y: 1 },
      type: 'raw',
    })
  })

  it('adds, updates and removes a shape outline', () => {
    const store = new EditorStore(structuredClone(document))

    store.svgNodeEditing.addShapeStroke('wheel')
    store.svgNodeEditing.setShapeStrokeColor('wheel', '#6C3BFF')
    store.svgNodeEditing.setShapeStrokeWidth('wheel', 4)
    store.setSelectedNode('wheel')

    expect(store.selectedNode).toMatchObject({
      style: {
        stroke: { type: 'color', value: '#6C3BFF' },
        strokeWidth: 4,
      },
    })

    store.svgNodeEditing.removeShapeStroke('wheel')

    expect(store.selectedNode).toMatchObject({
      style: { stroke: { type: 'none' }, strokeWidth: 0 },
    })
  })

  it('does not apply invalid gradient or stroke values', () => {
    const store = new EditorStore(structuredClone(document))

    store.svgNodeEditing.setGradientStopColor('wheel-gradient', 0, '')
    store.svgNodeEditing.setGradientStopColor('wheel-gradient', 0, '#invalid')
    store.svgNodeEditing.setShapeStrokeColor('wheel', '')
    store.svgNodeEditing.setShapeStrokeWidth('wheel', -1)

    expect(store.document.definitions?.gradients[0]?.stops[0]?.color).toBe(
      '#FF0000',
    )
    expect(store.document.layers[0]).toMatchObject({
      children: [{ style: { stroke: { type: 'none' }, strokeWidth: 0 } }],
    })
  })

  it('changes editable text properties', () => {
    const store = new EditorStore(document)

    store.svgNodeEditing.setTextContent('title', 'Updated title')
    store.svgNodeEditing.setTextFillColor('title', '#6C3BFF')
    store.svgNodeEditing.setTextFontFamily('title', 'Roboto')
    store.svgNodeEditing.setTextFontSize('title', 36)
    store.svgNodeEditing.setTextFontStyle('title', 'italic')
    store.svgNodeEditing.setTextFontWeight('title', 700)
    store.svgNodeEditing.setTextAnchor('title', 'start')
    store.svgNodeEditing.setTextPosition('title', 30, 40)
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
