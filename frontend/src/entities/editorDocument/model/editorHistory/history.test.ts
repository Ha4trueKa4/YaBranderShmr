import { describe, expect, it } from 'vitest'

import {
  commitHistory,
  createHistory,
  redoHistory,
  undoHistory,
} from './history'

describe('document history', () => {
  it('moves states between past and future', () => {
    const initialHistory = createHistory('first')
    const changedHistory = commitHistory(initialHistory, 'second', 50)
    const undoneHistory = undoHistory(changedHistory)

    expect(undoneHistory).toEqual({
      future: ['second'],
      past: [],
      present: 'first',
    })
    expect(undoneHistory && redoHistory(undoneHistory, 50)).toEqual(
      changedHistory,
    )
  })

  it('drops the oldest state when the limit is reached', () => {
    const firstChange = commitHistory(createHistory('first'), 'second', 2)
    const secondChange = commitHistory(firstChange, 'third', 2)
    const thirdChange = commitHistory(secondChange, 'fourth', 2)

    expect(thirdChange.past).toEqual(['second', 'third'])
  })

  it('clears the redo branch after a new change', () => {
    const changedHistory = commitHistory(createHistory('first'), 'second', 50)
    const undoneHistory = undoHistory(changedHistory)

    if (!undoneHistory) {
      throw new Error('Expected an undoable history')
    }

    expect(commitHistory(undoneHistory, 'third', 50)).toEqual({
      future: [],
      past: ['first'],
      present: 'third',
    })
  })

  it('supports falsy states', () => {
    const history = commitHistory(createHistory(0), 1, 50)

    expect(undoHistory(history)?.present).toBe(0)
  })
})
