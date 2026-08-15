import type { ShapeStyle, TextStyle } from '../types'

import { parseNumberAttribute } from './parseNumber'
import { parsePaint } from './parsePaint'

function parseOpacity(element: Element, attributeName: string): number {
  return parseNumberAttribute(element, attributeName, 1)
}

export function parseShapeStyle(element: Element): ShapeStyle {
  const strokeLineCap = element.getAttribute('stroke-linecap')
  const strokeLineJoin = element.getAttribute('stroke-linejoin')

  return {
    fill: parsePaint(element.getAttribute('fill'), {
      type: 'color',
      value: '#000000',
    }),
    fillOpacity: parseOpacity(element, 'fill-opacity'),
    stroke: parsePaint(element.getAttribute('stroke'), { type: 'none' }),
    ...(strokeLineCap === 'butt' ||
    strokeLineCap === 'round' ||
    strokeLineCap === 'square'
      ? { strokeLineCap }
      : {}),
    ...(strokeLineJoin === 'bevel' ||
    strokeLineJoin === 'miter' ||
    strokeLineJoin === 'round'
      ? { strokeLineJoin }
      : {}),
    strokeOpacity: parseOpacity(element, 'stroke-opacity'),
    strokeWidth: parseNumberAttribute(element, 'stroke-width', 0),
  }
}

export function parseTextStyle(element: Element): TextStyle {
  const fontStyle = element.getAttribute('font-style')
  const textAnchor = element.getAttribute('text-anchor')
  const fontWeight = element.getAttribute('font-weight')
  const letterSpacing = element.getAttribute('letter-spacing')

  return {
    fill: parsePaint(element.getAttribute('fill'), {
      type: 'color',
      value: '#000000',
    }),
    fontFamily: element.getAttribute('font-family') ?? 'sans-serif',
    fontSize: parseNumberAttribute(element, 'font-size', 16),
    ...(fontStyle === 'italic' || fontStyle === 'normal' ? { fontStyle } : {}),
    ...(fontWeight && Number.isFinite(Number(fontWeight))
      ? { fontWeight: Number(fontWeight) }
      : {}),
    ...(letterSpacing && Number.isFinite(Number(letterSpacing))
      ? { letterSpacing: Number(letterSpacing) }
      : {}),
    ...(textAnchor === 'end' ||
    textAnchor === 'middle' ||
    textAnchor === 'start'
      ? { textAnchor }
      : {}),
  }
}
