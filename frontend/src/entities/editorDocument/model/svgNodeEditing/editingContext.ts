import type { EditorDocument } from '../types'

export interface SvgEditingContext {
  applyDocumentChange: (change: () => void) => boolean
  getDocument: () => EditorDocument
}
