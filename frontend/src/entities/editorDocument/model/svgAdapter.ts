import { parseSvg } from './svgAdapter/parseSvg'
import type { EditorDocument } from './types'

export { parseSvg }

export interface SvgAdapter {
  parse: (svg: string) => EditorDocument
  serialize: (document: EditorDocument) => string
}
