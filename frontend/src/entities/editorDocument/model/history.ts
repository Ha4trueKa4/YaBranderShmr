export interface EditorHistory<TState> {
  future: TState[]
  past: TState[]
  present: TState
}
