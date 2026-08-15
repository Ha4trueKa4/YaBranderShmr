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
})
