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

    setTextRotation(
      nodeId: string,
      rotation: number,
      rotationCenter: { x: number; y: number },
    ) {
      const node = getEditableText(getDocument(), nodeId)

      if (
        !node ||
        !Number.isFinite(rotation) ||
        !Number.isFinite(rotationCenter.x) ||
        !Number.isFinite(rotationCenter.y)
      ) {
        return
      }

      applyDocumentChange(() => {
        node.transform.rotation = rotation
        node.transform.rotationCenter = rotationCenter
      })
    },

    setTextTransform(
      nodeId: string,
      transform: {
        rotationCenter?: { x: number; y: number }
        scaleX: number
        scaleY: number
        x: number
        y: number
      },
    ) {
      const node = getEditableText(getDocument(), nodeId)

      if (
        !node ||
        !Number.isFinite(transform.x) ||
        !Number.isFinite(transform.y) ||
        !Number.isFinite(transform.scaleX) ||
        !Number.isFinite(transform.scaleY) ||
        transform.scaleX <= 0 ||
        transform.scaleY <= 0 ||
        (transform.rotationCenter &&
          (!Number.isFinite(transform.rotationCenter.x) ||
            !Number.isFinite(transform.rotationCenter.y)))
      ) {
        return
      }

      applyDocumentChange(() => {
        node.transform.x = transform.x
        node.transform.y = transform.y
        node.transform.scaleX = transform.scaleX
        node.transform.scaleY = transform.scaleY

        if (transform.rotationCenter) {
          node.transform.rotationCenter = transform.rotationCenter
        }
      })
    },
  }
}
