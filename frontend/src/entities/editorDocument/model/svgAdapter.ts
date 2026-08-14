import type { EditorDocument } from './types'

export function parseSvg(_svg: string): EditorDocument {
  throw new Error('SVG parsing is not implemented yet')
}

export interface SvgAdapter {
  parse: (svg: string) => EditorDocument
  serialize: (document: EditorDocument) => string
}
