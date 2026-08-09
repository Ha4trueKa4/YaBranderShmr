import type { EditorDocument } from './types'

export type EditorDocumentSelector<TResult> = (
  document: EditorDocument,
) => TResult
