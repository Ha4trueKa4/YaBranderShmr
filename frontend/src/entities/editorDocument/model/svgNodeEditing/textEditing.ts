import type { TextStyle } from '../types'
import { getEditableText } from './editableNode'
import type { SvgEditingContext } from './editingContext'

const maximumTextLength = 1_000

export function createTextEditing({
  applyDocumentChange,
  getDocument,
}: SvgEditingContext) {
  return {
    setTextAnchor(
      nodeId: string,
      textAnchor: NonNullable<TextStyle['textAnchor']>,
    ) {
      const node = getEditableText(getDocument(), nodeId)

      if (node) {
        applyDocumentChange(() => {
          node.style.textAnchor = textAnchor
        })
      }
    },

    setTextContent(nodeId: string, text: string) {
      const node = getEditableText(getDocument(), nodeId)

      if (node) {
        applyDocumentChange(() => {
          node.text = text.slice(0, maximumTextLength)
        })
      }
    },

    setTextFillColor(nodeId: string, color: string) {
      const node = getEditableText(getDocument(), nodeId)

      if (node) {
        applyDocumentChange(() => {
          node.style.fill = { type: 'color', value: color }
        })
      }
    },

    setTextFontFamily(nodeId: string, fontFamily: string) {
      const node = getEditableText(getDocument(), nodeId)

      if (node) {
        applyDocumentChange(() => {
          node.style.fontFamily = fontFamily
        })
      }
    },

    setTextFontSize(nodeId: string, fontSize: number) {
      const node = getEditableText(getDocument(), nodeId)

      if (node && Number.isFinite(fontSize) && fontSize > 0) {
        applyDocumentChange(() => {
          node.style.fontSize = fontSize
        })
      }
    },

    setTextFontStyle(
      nodeId: string,
      fontStyle: NonNullable<TextStyle['fontStyle']>,
    ) {
      const node = getEditableText(getDocument(), nodeId)

      if (node) {
        applyDocumentChange(() => {
          node.style.fontStyle = fontStyle
        })
      }
    },

    setTextFontWeight(nodeId: string, fontWeight: number) {
      const node = getEditableText(getDocument(), nodeId)

      if (node && Number.isFinite(fontWeight) && fontWeight > 0) {
        applyDocumentChange(() => {
          node.style.fontWeight = fontWeight
        })
      }
    },

    setTextPosition(nodeId: string, x: number, y: number) {
      const node = getEditableText(getDocument(), nodeId)

      if (node && Number.isFinite(x) && Number.isFinite(y)) {
        applyDocumentChange(() => {
          node.x = x
          node.y = y
        })
      }
    },
  }
}
