import { getEditableShape } from './editableNode'
import type { SvgEditingContext } from './editingContext'

const hexColorPattern = /^#(?:[\dA-Fa-f]{3}|[\dA-Fa-f]{6})$/

function isValidHexColor(color: string) {
  return hexColorPattern.test(color)
}

export function createShapeEditing({
  applyDocumentChange,
  getDocument,
}: SvgEditingContext) {
  return {
    addShapeStroke(nodeId: string) {
      const node = getEditableShape(getDocument(), nodeId)

      if (!node) {
        return
      }

      const currentStrokeColor =
        node.style.stroke.type === 'color' ? node.style.stroke.value : null
      const currentStrokeWidth = node.style.strokeWidth

      if (
        (currentStrokeColor !== null && !isValidHexColor(currentStrokeColor)) ||
        !Number.isFinite(currentStrokeWidth) ||
        currentStrokeWidth < 0
      ) {
        return
      }

      applyDocumentChange(() => {
        node.style.stroke = {
          type: 'color',
          value: currentStrokeColor ?? '#000000',
        }
        node.style.strokeWidth = Math.max(node.style.strokeWidth, 1)
      })
    },

    removeShapeStroke(nodeId: string) {
      const node = getEditableShape(getDocument(), nodeId)

      if (!node) {
        return
      }

      applyDocumentChange(() => {
        node.style.stroke = { type: 'none' }
        node.style.strokeWidth = 0
      })
    },

    setShapeFillColor(nodeId: string, color: string) {
      const node = getEditableShape(getDocument(), nodeId)

      if (!node) {
        return
      }

      applyDocumentChange(() => {
        node.style.fill = { type: 'color', value: color }
      })
    },

    setShapeFillOpacity(nodeId: string, opacity: number) {
      const node = getEditableShape(getDocument(), nodeId)

      if (!node || !Number.isFinite(opacity)) {
        return
      }

      applyDocumentChange(() => {
        node.style.fillOpacity = Math.min(Math.max(opacity, 0), 1)
      })
    },

    setShapeOpacity(nodeId: string, opacity: number) {
      const node = getEditableShape(getDocument(), nodeId)

      if (!node || !Number.isFinite(opacity)) {
        return
      }

      applyDocumentChange(() => {
        node.opacity = Math.min(Math.max(opacity, 0), 1)
      })
    },

    setShapePosition(nodeId: string, x: number, y: number) {
      const node = getEditableShape(getDocument(), nodeId)

      if (!node || !Number.isFinite(x) || !Number.isFinite(y)) {
        return
      }

      applyDocumentChange(() => {
        node.transform.x = x
        node.transform.y = y
      })
    },

    setShapeRotation(
      nodeId: string,
      rotation: number,
      rotationCenter: { x: number; y: number },
    ) {
      const node = getEditableShape(getDocument(), nodeId)

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

    setShapeTransform(
      nodeId: string,
      transform: {
        rotationCenter?: { x: number; y: number }
        scaleX: number
        scaleY: number
        x: number
        y: number
      },
    ) {
      const node = getEditableShape(getDocument(), nodeId)

      if (
        !node ||
        !Number.isFinite(transform.x) ||
        !Number.isFinite(transform.y) ||
        !Number.isFinite(transform.scaleX) ||
        !Number.isFinite(transform.scaleY) ||
        (transform.rotationCenter &&
          (!Number.isFinite(transform.rotationCenter.x) ||
            !Number.isFinite(transform.rotationCenter.y))) ||
        transform.scaleX <= 0 ||
        transform.scaleY <= 0
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

    setShapeStrokeColor(nodeId: string, color: string) {
      const node = getEditableShape(getDocument(), nodeId)

      if (!node || !isValidHexColor(color)) {
        return
      }

      applyDocumentChange(() => {
        node.style.stroke = { type: 'color', value: color }
        node.style.strokeWidth = Math.max(node.style.strokeWidth, 1)
      })
    },

    setShapeStrokeOpacity(nodeId: string, opacity: number) {
      const node = getEditableShape(getDocument(), nodeId)

      if (!node || !Number.isFinite(opacity)) {
        return
      }

      applyDocumentChange(() => {
        node.style.strokeOpacity = Math.min(Math.max(opacity, 0), 1)
      })
    },

    setShapeStrokeWidth(nodeId: string, strokeWidth: number) {
      const node = getEditableShape(getDocument(), nodeId)

      if (!node || !Number.isFinite(strokeWidth) || strokeWidth < 0) {
        return
      }

      applyDocumentChange(() => {
        node.style.strokeWidth = strokeWidth
      })
    },
  }
}
