import { describe, expect, it } from 'vitest'
import { constrainViewportOffset } from './constrainViewportOffset'

describe('constrainViewportOffset', () => {
  it('keeps at least 48 pixels of the artboard visible', () => {
    expect(
      constrainViewportOffset(
        { x: 900, y: -900 },
        { height: 600, width: 800 },
        { height: 300, width: 400 },
        1,
      ),
    ).toEqual({ x: 552, y: -402 })
  })

  it('uses the scaled artboard size for the constraint', () => {
    expect(
      constrainViewportOffset(
        { x: 900, y: -900 },
        { height: 600, width: 800 },
        { height: 300, width: 400 },
        0.5,
      ),
    ).toEqual({ x: 452, y: -327 })
  })
})
