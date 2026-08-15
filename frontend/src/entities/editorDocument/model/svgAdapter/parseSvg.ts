import type { EditorDocument } from '../types'

import { SvgParseError } from './errors'
import { parseDefinitions } from './parsers/parseDefinitions'
import { parseRoot } from './parseRoot'
import { validateSvgElements } from './validateSvg'

export function parseSvg(svg: string): EditorDocument {
  const xml = new DOMParser().parseFromString(svg, 'image/svg+xml')
  const parserError = xml.querySelector('parsererror')

  if (parserError) {
    throw new SvgParseError('Invalid SVG markup')
  }

  const canvas = parseRoot(xml.documentElement)
  validateSvgElements(xml.documentElement)
  const definitions = parseDefinitions(xml.documentElement)

  return { ...canvas, ...(definitions ? { definitions } : {}), layers: [] }
}
