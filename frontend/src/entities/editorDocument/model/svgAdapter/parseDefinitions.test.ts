import { describe, expect, it } from 'vitest'

import { parseDefinitions } from './parsers/parseDefinitions'

function parseRoot(svg: string): Element {
  return new DOMParser().parseFromString(svg, 'image/svg+xml').documentElement
}

describe('parseDefinitions', () => {
  it('preserves gradient parameters and stops without turning them into layers', () => {
    const definitions = parseDefinitions(
      parseRoot(`
        <svg xmlns="http://www.w3.org/2000/svg">
          <defs>
            <linearGradient id="bodyGradient" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stop-color="#FFE44D" />
              <stop offset="100%" stop-color="#E6B800" stop-opacity="0.8" />
            </linearGradient>
          </defs>
        </svg>
      `),
    )

    expect(definitions).toEqual({
      gradients: [
        {
          attributes: { x1: '0%', x2: '0%', y1: '0%', y2: '100%' },
          id: 'bodyGradient',
          stops: [
            { color: '#FFE44D', offset: '0%' },
            { color: '#E6B800', offset: '100%', opacity: 0.8 },
          ],
          type: 'linearGradient',
        },
      ],
    })
  })

  it('returns undefined when SVG has no supported gradients', () => {
    expect(
      parseDefinitions(
        parseRoot('<svg xmlns="http://www.w3.org/2000/svg"><defs /></svg>'),
      ),
    ).toBeUndefined()
  })

  it('requires a stable gradient identifier', () => {
    expect(() =>
      parseDefinitions(
        parseRoot(`
          <svg xmlns="http://www.w3.org/2000/svg">
            <defs><radialGradient><stop offset="0%" stop-color="#000" /></radialGradient></defs>
          </svg>
        `),
      ),
    ).toThrow('SVG <radialGradient> must define a stable id')
  })
})
