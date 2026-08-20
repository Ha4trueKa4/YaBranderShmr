import { describe, expect, it } from 'vitest'

import { constrainSelectionBounds } from './constrainSelectionBounds'

describe('constrainSelectionBounds', () => {
  it('keeps the selection outline inside the canvas for a full-canvas background', () => {
    expect(
      constrainSelectionBounds(
        { height: 500, width: 500, x: 0, y: 0 },
        500,
        500,
      ),
    ).toEqual({
      height: 497,
      width: 497,
      x: 1.5,
      y: 1.5,
    })
  })

  it('preserves padding around selections away from canvas edges', () => {
    expect(
      constrainSelectionBounds(
        { height: 20, width: 30, x: 100, y: 200 },
        500,
        500,
      ),
    ).toEqual({
      height: 28,
      width: 38,
      x: 96,
      y: 196,
    })
  })

  it('uses the viewBox origin as the canvas boundary', () => {
    expect(
      constrainSelectionBounds(
        { height: 300, width: 500, x: -250, y: 100 },
        500,
        300,
        -250,
        100,
      ),
    ).toEqual({
      height: 297,
      width: 497,
      x: -248.5,
      y: 101.5,
    })
  })
})
