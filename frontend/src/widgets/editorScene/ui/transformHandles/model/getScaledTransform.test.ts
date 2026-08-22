import { describe, expect, it } from 'vitest'

import { getScaledTransform } from './getScaledTransform'

const bounds = { height: 50, width: 100, x: 10, y: 20 }
const transform = { rotation: 0, scaleX: 1, scaleY: 1, x: 0, y: 0 }

describe('getScaledTransform', () => {
  it('keeps the opposite corner fixed when a corner handle scales a shape', () => {
    expect(
      getScaledTransform(bounds, 'southEast', transform, { x: 210, y: 120 }),
    ).toEqual({ scaleX: 2, scaleY: 2, x: -10, y: -20 })
  })

  it('compensates position when scaling from the north-west corner', () => {
    expect(
      getScaledTransform(bounds, 'northWest', transform, { x: 60, y: 45 }),
    ).toEqual({ scaleX: 0.5, scaleY: 0.5, x: 55, y: 35 })
  })

  it('changes only one axis for a side handle', () => {
    expect(
      getScaledTransform(bounds, 'east', transform, { x: 160, y: 999 }),
    ).toEqual({ scaleX: 1.5, scaleY: 1, x: -5, y: 0 })
  })

  it('scales from the center when Shift is held on a side handle', () => {
    expect(
      getScaledTransform(
        bounds,
        'east',
        transform,
        { x: 160, y: 999 },
        { scaleFromCenter: true },
      ),
    ).toEqual({ scaleX: 2, scaleY: 1, x: -60, y: 0 })
  })

  it('keeps proportions when Shift is held on a corner handle', () => {
    expect(
      getScaledTransform(
        bounds,
        'southEast',
        transform,
        { x: 210, y: 70 },
        { preserveAspectRatio: true, scaleFromCenter: true },
      ),
    ).toEqual({ scaleX: 3, scaleY: 3, x: -120, y: -90 })
  })
})
