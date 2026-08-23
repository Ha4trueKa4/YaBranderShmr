import type { EditorDocument } from '../types'
import { deduplicateSvgAttributes } from './deduplicateAttributes'
import { SvgParseError } from './errors'
import { normalizeSvg } from './normalizeSvg'
import { parseRoot } from './parseRoot'
import { parseDefinitionsTolerant } from './parsers/parseDefinitions'
import { parseLayersTolerant } from './parsers/parseLayers'
import { sanitizeSvg } from './sanitizeSvg'
import type { SvgImportResult } from './types'

export function importSvg(svg: string): SvgImportResult {
  const { svg: normalizedAttributesSvg, warnings: attributeWarnings } =
    deduplicateSvgAttributes(svg)
  const xml = new DOMParser().parseFromString(
    normalizedAttributesSvg,
    'image/svg+xml',
  )
  const parserError = xml.querySelector('parsererror')

  if (parserError) {
    throw new SvgParseError('Invalid SVG markup')
  }

  const warnings = [
    ...attributeWarnings,
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
