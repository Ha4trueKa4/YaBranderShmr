import { toJS } from 'mobx'

import type { EditorDocument } from '../types'

export const editorHistoryLimit = 50

export function createDocumentSnapshot(
  document: EditorDocument,
): EditorDocument {
  return structuredClone(toJS(document))
}

export function areDocumentsEqual(
  first: EditorDocument,
  second: EditorDocument,
): boolean {
  return JSON.stringify(first) === JSON.stringify(second)
}
