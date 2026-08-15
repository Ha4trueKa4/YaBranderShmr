import { parseSvg } from '@entities/editorDocument'
import { describe, expect, it } from 'vitest'

import { mockTaxiSvg } from './mockTaxiSvg'

describe('mockTaxiSvg', () => {
  it('is converted into a canvas and an editable taxi layer tree', () => {
    expect(parseSvg(mockTaxiSvg)).toMatchObject({
      height: 500,
      layers: [
        { id: 'canvas-background', role: 'background', type: 'shape' },
        {
          children: [
            { id: 'taxi-body', type: 'shape' },
            { id: 'taxi-window', type: 'shape' },
            { id: 'rear-wheel', type: 'shape' },
            { id: 'front-wheel', type: 'shape' },
            {
              id: 'taxi-sign',
              transform: { x: 250, y: 150 },
              type: 'group',
            },
          ],
          id: 'taxi',
          type: 'group',
        },
      ],
      width: 500,
    })
  })
})
