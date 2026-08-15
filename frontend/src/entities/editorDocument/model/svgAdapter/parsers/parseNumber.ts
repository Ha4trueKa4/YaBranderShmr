import { SvgParseError } from '../errors'

export function parseNumberAttribute(
  element: Element,
  attributeName: string,
  fallback?: number,
): number {
  const value = element.getAttribute(attributeName)

  if (value === null) {
    if (fallback !== undefined) {
      return fallback
    }

    throw new SvgParseError(
      `SVG element <${element.localName}> must define ${attributeName}`,
    )
  }

  const parsedValue = Number(value)

  if (!Number.isFinite(parsedValue)) {
    throw new SvgParseError(
      `SVG attribute ${attributeName} on <${element.localName}> must be a number`,
    )
  }

  return parsedValue
}
