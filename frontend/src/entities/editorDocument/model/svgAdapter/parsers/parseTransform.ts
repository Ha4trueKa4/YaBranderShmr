import type { Transform } from '../../types'

import { SvgParseError } from '../errors'

const identityTransform: Transform = {
  rotation: 0,
  scaleX: 1,
  scaleY: 1,
  x: 0,
  y: 0,
}

const transformExpression = /([a-zA-Z]+)\(([^)]*)\)/g

function parseTransformValues(value: string): number[] {
  const values = value
    .trim()
    .split(/[\s,]+/)
    .filter(Boolean)
    .map(Number)

  if (values.some((item) => !Number.isFinite(item))) {
    throw new SvgParseError('SVG transform must contain only finite numbers')
  }

  return values
}

export function parseTransform(value: string | null): Transform {
  if (!value) {
    return identityTransform
  }

  const transform: Transform = { ...identityTransform }
  let parsedEnd = 0

  for (const match of value.matchAll(transformExpression)) {
    const [expression, name, rawValues] = match
    const values = parseTransformValues(rawValues)
    const expressionStart = match.index ?? 0

    if (!/^[\s,]*$/.test(value.slice(parsedEnd, expressionStart))) {
      throw new SvgParseError(`Invalid SVG transform: ${value}`)
    }

    parsedEnd = expressionStart + expression.length

    if (name === 'translate' && (values.length === 1 || values.length === 2)) {
      transform.x += values[0]
      transform.y += values[1] ?? 0
      continue
    }

    if (name === 'scale' && (values.length === 1 || values.length === 2)) {
      transform.scaleX *= values[0]
      transform.scaleY *= values[1] ?? values[0]
      continue
    }

    if (name === 'rotate' && (values.length === 1 || values.length === 3)) {
      transform.rotation += values[0]
      continue
    }

    throw new SvgParseError(`Unsupported SVG transform: ${expression}`)
  }

  if (!/^[\s,]*$/.test(value.slice(parsedEnd))) {
    throw new SvgParseError(`Invalid SVG transform: ${value}`)
  }

  return transform
}
