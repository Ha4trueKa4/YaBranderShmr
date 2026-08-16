import type {
  EditorDefinitions,
  GradientDefinition,
  GradientStop,
} from '../../types'

import { SvgParseError } from '../errors'

const supportedGradientNames = new Set(['linearGradient', 'radialGradient'])

function collectAttributes(element: Element): Record<string, string> {
  return Array.from(element.attributes).reduce<Record<string, string>>(
    (attributes, attribute) => {
      if (attribute.name !== 'id') {
        attributes[attribute.name] = attribute.value
      }

      return attributes
    },
    {},
  )
}

function parseGradientStop(element: Element): GradientStop {
  const offset = element.getAttribute('offset')
  const color = element.getAttribute('stop-color')

  if (offset === null || color === null) {
    throw new SvgParseError(
      'SVG gradient stop must define offset and stop-color attributes',
    )
  }

  const opacity = element.getAttribute('stop-opacity')

  return {
    color,
    offset,
    ...(opacity === null ? {} : { opacity: Number(opacity) }),
  }
}

function parseGradient(element: Element): GradientDefinition {
  const id = element.getAttribute('id')

  if (!id) {
    throw new SvgParseError(
      `SVG <${element.localName}> must define a stable id`,
    )
  }

  return {
    attributes: collectAttributes(element),
    id,
    stops: Array.from(element.children)
      .filter((child) => child.localName === 'stop')
      .map(parseGradientStop),
    type: element.localName as GradientDefinition['type'],
  }
}

export function parseDefinitions(root: Element): EditorDefinitions | undefined {
  const gradients = Array.from(root.children)
    .filter((child) => child.localName === 'defs')
    .flatMap((definitions) => Array.from(definitions.children))
    .filter((element) => supportedGradientNames.has(element.localName))
    .map(parseGradient)

  return gradients.length > 0 ? { gradients } : undefined
}

export function parseDefinitionsTolerant(
  root: Element,
): EditorDefinitions | undefined {
  const gradients: GradientDefinition[] = []
  const rawDefinitions: string[] = []

  for (const definition of Array.from(root.children).filter(
    (child) => child.localName === 'defs',
  )) {
    for (const element of Array.from(definition.children)) {
      if (!supportedGradientNames.has(element.localName)) {
        rawDefinitions.push(new XMLSerializer().serializeToString(element))
        continue
      }

      try {
        gradients.push(parseGradient(element))
      } catch (error) {
        if (!(error instanceof SvgParseError)) {
          throw error
        }

        rawDefinitions.push(new XMLSerializer().serializeToString(element))
      }
    }
  }

  if (gradients.length === 0 && rawDefinitions.length === 0) {
    return undefined
  }

  return {
    gradients,
    ...(rawDefinitions.length > 0 ? { rawSvg: rawDefinitions.join('') } : {}),
  }
}
