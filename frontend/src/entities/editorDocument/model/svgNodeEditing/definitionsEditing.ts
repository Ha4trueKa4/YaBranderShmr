import type { SvgEditingContext } from './editingContext'

const hexColorPattern = /^#(?:[\dA-Fa-f]{3}|[\dA-Fa-f]{6})$/

function isValidHexColor(color: string) {
  return hexColorPattern.test(color)
}

export function createDefinitionsEditing({
  applyDocumentChange,
  getDocument,
}: SvgEditingContext) {
  return {
    setGradientStopColor(gradientId: string, stopIndex: number, color: string) {
      const gradient = getDocument().definitions?.gradients.find(
        (item) => item.id === gradientId,
      )

      if (
        !gradient ||
        !Number.isInteger(stopIndex) ||
        stopIndex < 0 ||
        stopIndex >= gradient.stops.length ||
        !isValidHexColor(color)
      ) {
        return
      }

      applyDocumentChange(() => {
        gradient.stops[stopIndex].color = color
      })
    },
  }
}
