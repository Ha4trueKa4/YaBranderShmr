import { importSvg, parseSvg } from './svgAdapter/parseSvg'
import { serializeSvg } from './svgAdapter/serializeSvg'
import type { SvgImportResult, SvgImportWarning } from './svgAdapter/types'
import type { EditorDocument } from './types'

export type { SvgImportResult, SvgImportWarning }
export { importSvg, parseSvg, serializeSvg }

export interface SvgAdapter {
  parse: (svg: string) => EditorDocument
  serialize: (document: EditorDocument) => string
}
