import { parseSvg } from '@entities/editorDocument'
import { describe, expect, it } from 'vitest'

import { getLayerPresentation } from '../layerPresentation'

describe('getLayerPresentation', () => {
  it('returns a distinct presentation for each supported layer type', () => {
    const document = parseSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        <g id="group"><rect id="rectangle" width="10" height="10" /></g>
        <circle id="circle" r="10" />
        <ellipse id="ellipse" rx="10" ry="5" />
        <line id="line" />
        <path id="path" d="M0 0 L10 10" />
        <text id="text">Label</text>
      </svg>
    `)

    expect(document.layers.map(getLayerPresentation)).toMatchObject([
      { label: 'Group' },
      { label: 'Circle' },
      { label: 'Ellipse' },
      { label: 'Line' },
      { label: 'Path' },
      { label: 'Text' },
    ])
    const group = document.layers[0]

    if (group.type !== 'group') {
      throw new Error('Expected the first test layer to be a group')
    }

    expect(getLayerPresentation(group.children[0]).label).toBe('Rectangle')
  })
})
