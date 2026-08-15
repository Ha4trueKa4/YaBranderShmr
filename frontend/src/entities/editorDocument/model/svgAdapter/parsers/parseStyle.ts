import type { ShapeStyle, TextStyle } from '../../types'

import { SvgParseError } from '../errors'
import { parseNumberAttribute } from './parseNumber'
import { parsePaint } from './parsePaint'

function parseOpacity(element: Element, attributeName: string): number {
  return parseNumberAttribute(element, attributeName, 1)
}

function parseFontWeight(value: string | null): number | undefined {
  if (value === null) {
    return undefined
  }

  if (value === 'normal') {
    return 400
  }

  if (value === 'bold') {
    return 700
  }

  const fontWeight = Number(value)

  if (!Number.isFinite(fontWeight)) {
    throw new SvgParseError(`Unsupported SVG font-weight: ${value}`)
  }

  return fontWeight
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
  const fontWeight = parseFontWeight(element.getAttribute('font-weight'))
  const letterSpacing = element.getAttribute('letter-spacing')

  return {
    fill: parsePaint(element.getAttribute('fill'), {
      type: 'color',
      value: '#000000',
    }),
    fontFamily: element.getAttribute('font-family') ?? 'sans-serif',
    fontSize: parseNumberAttribute(element, 'font-size', 16),
    ...(fontStyle === 'italic' || fontStyle === 'normal' ? { fontStyle } : {}),
    ...(fontWeight === undefined ? {} : { fontWeight }),
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
