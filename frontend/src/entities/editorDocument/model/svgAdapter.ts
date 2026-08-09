import type { EditorDocument } from './types'

export interface SvgAdapter {
  parse: (svg: string) => EditorDocument
  serialize: (document: EditorDocument) => string
}
