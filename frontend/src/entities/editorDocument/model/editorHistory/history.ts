export interface EditorHistory<TState> {
  future: TState[]
  past: TState[]
  present: TState
}

export function createHistory<TState>(
  initialState: TState,
): EditorHistory<TState> {
  return { future: [], past: [], present: initialState }
}

export function commitHistory<TState>(
  history: EditorHistory<TState>,
  nextState: TState,
  limit: number,
): EditorHistory<TState> {
  return {
    future: [],
    past: [...history.past, history.present].slice(-limit),
    present: nextState,
  }
}

export function undoHistory<TState>(
  history: EditorHistory<TState>,
): EditorHistory<TState> | null {
  const previousState = history.past.at(-1)

  if (previousState === undefined) {
    return null
  }

  return {
    future: [history.present, ...history.future],
    past: history.past.slice(0, -1),
    present: previousState,
  }
}

export function redoHistory<TState>(
  history: EditorHistory<TState>,
  limit: number,
): EditorHistory<TState> | null {
  const nextState = history.future.at(0)

  if (nextState === undefined) {
    return null
  }

  return {
    future: history.future.slice(1),
    past: [...history.past, history.present].slice(-limit),
    present: nextState,
  }
}
