import type {
  EditorElementBase,
  EditorGroup,
  EditorNode,
  EditorShape,
  EditorText,
} from '../../types'

import { SvgParseError } from '../errors'
import { parseNumberAttribute } from './parseNumber'
import { parseShapeStyle, parseTextStyle } from './parseStyle'
import { parseTransform } from './parseTransform'

function parseBase(element: Element): EditorElementBase {
  const id = element.getAttribute('id')

  if (!id) {
    throw new SvgParseError(
      `SVG element <${element.localName}> must define a stable id`,
    )
  }

  const role = element.getAttribute('data-role')

  return {
    id,
    isLocked: false,
    isVisible: true,
    name: element.getAttribute('data-name') ?? id,
    opacity: parseNumberAttribute(element, 'opacity', 1),
    ...(role === 'background' || role === 'content' ? { role } : {}),
    transform: parseTransform(element.getAttribute('transform')),
  }
}

function parseShape(element: Element): EditorShape {
  const base = parseBase(element)
  const style = parseShapeStyle(element)

  switch (element.localName) {
    case 'rect':
      return {
        ...base,
        geometry: {
          height: parseNumberAttribute(element, 'height'),
          ...(element.hasAttribute('rx')
            ? { rx: parseNumberAttribute(element, 'rx') }
            : {}),
          ...(element.hasAttribute('ry')
            ? { ry: parseNumberAttribute(element, 'ry') }
            : {}),
          type: 'rect',
          width: parseNumberAttribute(element, 'width'),
          x: parseNumberAttribute(element, 'x', 0),
          y: parseNumberAttribute(element, 'y', 0),
        },
        shapeType: 'rect',
        style,
        type: 'shape',
      }
    case 'circle':
      return {
        ...base,
        geometry: {
          cx: parseNumberAttribute(element, 'cx', 0),
          cy: parseNumberAttribute(element, 'cy', 0),
          r: parseNumberAttribute(element, 'r'),
          type: 'circle',
        },
        shapeType: 'circle',
        style,
        type: 'shape',
      }
    case 'ellipse':
      return {
        ...base,
        geometry: {
          cx: parseNumberAttribute(element, 'cx', 0),
          cy: parseNumberAttribute(element, 'cy', 0),
          rx: parseNumberAttribute(element, 'rx'),
          ry: parseNumberAttribute(element, 'ry'),
          type: 'ellipse',
        },
        shapeType: 'ellipse',
        style,
        type: 'shape',
      }
    case 'line':
      return {
        ...base,
        geometry: {
          type: 'line',
          x1: parseNumberAttribute(element, 'x1', 0),
          x2: parseNumberAttribute(element, 'x2', 0),
          y1: parseNumberAttribute(element, 'y1', 0),
          y2: parseNumberAttribute(element, 'y2', 0),
        },
        shapeType: 'line',
        style,
        type: 'shape',
      }
    case 'path': {
      const d = element.getAttribute('d')

      if (!d) {
        throw new SvgParseError('SVG <path> must define d')
      }

      return {
        ...base,
        geometry: { d, type: 'path' },
        shapeType: 'path',
        style,
        type: 'shape',
      }
    }
    default:
      throw new SvgParseError(`Unsupported SVG element: ${element.localName}`)
  }
}

function parseText(element: Element): EditorText {
  return {
    ...parseBase(element),
    style: parseTextStyle(element),
    text: element.textContent ?? '',
    type: 'text',
    x: parseNumberAttribute(element, 'x', 0),
    y: parseNumberAttribute(element, 'y', 0),
  }
}

function parseNode(element: Element): EditorNode {
  if (element.localName === 'g') {
    const group: EditorGroup = {
      ...parseBase(element),
      children: Array.from(element.children).map(parseNode),
      type: 'group',
    }

    return group
  }

  if (element.localName === 'text') {
    return parseText(element)
  }

  return parseShape(element)
}

export function parseLayers(root: Element): EditorNode[] {
  return Array.from(root.children)
    .filter((element) => element.localName !== 'defs')
    .map(parseNode)
}
