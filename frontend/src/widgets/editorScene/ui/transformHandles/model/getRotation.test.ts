import { describe, expect, it } from 'vitest'

import { getPointerAngle, getRotation } from './getRotation'

describe('getRotation', () => {
  it('calculates the angle around the shape center', () => {
    expect(getPointerAngle({ x: 10, y: 10 }, { x: 10, y: 0 })).toBe(-90)
    expect(getPointerAngle({ x: 10, y: 10 }, { x: 20, y: 10 })).toBe(0)
  })

  it('adds the pointer movement to the initial rotation', () => {
    expect(getRotation(15, -90, 0)).toBe(105)
  })

  it('uses the shortest angle while crossing the negative-positive boundary', () => {
    expect(getRotation(0, 170, -170)).toBe(20)
  })
})
