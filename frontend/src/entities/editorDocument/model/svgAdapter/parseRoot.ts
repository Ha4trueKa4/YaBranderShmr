import { SvgParseError } from './errors'

export type SvgCanvas = {
  height: number
  width: number
}

export function parseRoot(root: Element): SvgCanvas {
  if (root.localName !== 'svg') {
    throw new SvgParseError('SVG document must have an <svg> root element')
  }

  if (root.namespaceURI !== 'http://www.w3.org/2000/svg') {
    throw new SvgParseError('SVG root must use the SVG namespace')
  }

  const viewBox = root.getAttribute('viewBox')

  if (!viewBox) {
    throw new SvgParseError('SVG root must define a viewBox')
  }

  const values = viewBox
    .trim()
    .split(/[\s,]+/)
    .map(Number)

  if (
    values.length !== 4 ||
    values.some((value) => !Number.isFinite(value)) ||
    values[2] <= 0 ||
    values[3] <= 0
  ) {
    throw new SvgParseError('SVG viewBox must contain four finite numbers')
  }

  return { height: values[3], width: values[2] }
}
