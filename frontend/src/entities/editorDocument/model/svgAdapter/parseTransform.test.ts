import { describe, expect, it } from 'vitest'

import { parseTransform } from './parseTransform'

describe('parseTransform', () => {
  it('returns the identity transform when the attribute is absent', () => {
    expect(parseTransform(null)).toEqual({
      rotation: 0,
      scaleX: 1,
      scaleY: 1,
      x: 0,
      y: 0,
    })
  })

  it('parses translate, rotate and scale operations', () => {
    expect(
      parseTransform('translate(20 10) rotate(5 250 250) scale(1.2 0.8)'),
    ).toEqual({ rotation: 5, scaleX: 1.2, scaleY: 0.8, x: 20, y: 10 })
  })

  it('rejects transforms that the editor cannot preserve', () => {
    expect(() => parseTransform('matrix(1 0 0 1 20 10)')).toThrow(
      'Unsupported SVG transform',
    )
  })
})
