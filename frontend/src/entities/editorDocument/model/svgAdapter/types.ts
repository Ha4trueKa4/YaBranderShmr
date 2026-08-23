import type { EditorDocument } from '../types'

export type SvgImportWarning =
  | {
      code: 'generated-editor-id'
      editorId: string
      elementName: string
    }
  | {
      code: 'missing-view-box'
      height: number
      width: number
    }
  | {
      code: 'removed-unsafe-attribute'
      attributeName: string
      elementName: string
    }
  | {
      code: 'removed-unsafe-element'
      elementName: string
    }
  | {
      attributeName: string
      code: 'ignored-duplicate-attribute'
      elementName: string
    }
  | {
      code: 'locked-raw-node'
      editorId: string
      elementName: string
      reason: string
    }

export interface SvgImportResult {
  document: EditorDocument
  warnings: SvgImportWarning[]
}
