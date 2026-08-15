import { SvgParseError } from './errors'

const unsupportedElementNames = new Set([
  'animate',
  'animateMotion',
  'animateTransform',
  'clipPath',
  'filter',
  'foreignObject',
  'iframe',
  'image',
  'mask',
  'marker',
  'script',
  'set',
  'style',
  'symbol',
  'tspan',
  'use',
])

export function validateSvgElements(root: Element) {
  for (const element of root.querySelectorAll('*')) {
    if (unsupportedElementNames.has(element.localName)) {
      throw new SvgParseError(`Unsupported SVG element: ${element.localName}`)
    }
  }
}
