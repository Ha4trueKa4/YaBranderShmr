import { describe, expect, it } from 'vitest'

import { getRotatedScaledTransform } from './getRotatedScaledTransform'

describe('getRotatedScaledTransform', () => {
  it('keeps the opposite corner fixed in the figure local axes', () => {
    expect(
      getRotatedScaledTransform(
        { height: 100, width: 100, x: 0, y: 0 },
        'southEast',
        {
          rotation: 90,
          rotationCenter: { x: 50, y: 50 },
          scaleX: 1,
          scaleY: 1,
          x: 0,
          y: 0,
        },
        { x: 200, y: 200 },
      ),
    ).toEqual({
      rotationCenter: { x: 100, y: 100 },
      scaleX: 2,
      scaleY: 2,
      x: -100,
      y: 0,
    })
  })

  it('keeps the rotation center fixed when Shift is held', () => {
    expect(
      getRotatedScaledTransform(
        { height: 100, width: 100, x: 0, y: 0 },
        'southEast',
        {
          rotation: 90,
          rotationCenter: { x: 50, y: 50 },
          scaleX: 1,
          scaleY: 1,
          x: 0,
          y: 0,
        },
        { x: 150, y: 150 },
        { preserveAspectRatio: true, scaleFromCenter: true },
      ),
    ).toEqual({
      rotationCenter: { x: 100, y: 100 },
      scaleX: 2,
      scaleY: 2,
      x: -50,
      y: -50,
    })
  })
})
