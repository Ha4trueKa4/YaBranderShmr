import type { EditorDocument } from '../types'

import { SvgParseError } from './errors'
import { normalizeSvg } from './normalizeSvg'
import { parseRoot } from './parseRoot'
import { parseDefinitionsTolerant } from './parsers/parseDefinitions'
import { parseLayersTolerant } from './parsers/parseLayers'
import { sanitizeSvg } from './sanitizeSvg'
import type { SvgImportResult } from './types'

export function importSvg(svg: string): SvgImportResult {
  const xml = new DOMParser().parseFromString(svg, 'image/svg+xml')
  const parserError = xml.querySelector('parsererror')

  if (parserError) {
    throw new SvgParseError('Invalid SVG markup')
  }

  const warnings = [
    ...sanitizeSvg(xml.documentElement),
    ...normalizeSvg(xml.documentElement),
  ]
  const canvas = parseRoot(xml.documentElement)
  const definitions = parseDefinitionsTolerant(xml.documentElement)
  const layers = parseLayersTolerant(xml.documentElement, warnings)

  return {
    document: {
      ...canvas,
      ...(definitions ? { definitions } : {}),
      layers,
    },
    warnings,
  }
}

export function parseSvg(svg: string): EditorDocument {
  return importSvg(svg).document
}
