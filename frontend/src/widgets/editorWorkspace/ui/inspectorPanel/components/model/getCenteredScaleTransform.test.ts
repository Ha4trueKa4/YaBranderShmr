import { describe, expect, it } from 'vitest'

import { getCenteredScaleTransform } from './getCenteredScaleTransform'

describe('getCenteredScaleTransform', () => {
  it('keeps the visual center fixed when increasing the uniform scale', () => {
    expect(
      getCenteredScaleTransform(
        {
          rotation: 0,
          scaleX: 1,
          scaleY: 1,
          x: 20,
          y: 30,
        },
        { x: 150, y: 75 },
        2,
      ),
    ).toEqual({
      rotationCenter: { x: 300, y: 150 },
      scaleX: 2,
      scaleY: 2,
      x: -130,
      y: -45,
    })
  })

  it('keeps an established rotation center while changing the scale', () => {
    expect(
      getCenteredScaleTransform(
        {
          rotation: 45,
          rotationCenter: { x: 300, y: 150 },
          scaleX: 2,
          scaleY: 2,
          x: -130,
          y: -45,
        },
        { x: 300, y: 150 },
        1,
      ),
    ).toEqual({
      rotationCenter: { x: 150, y: 75 },
      scaleX: 1,
      scaleY: 1,
      x: 20,
      y: 30,
    })
  })
})
