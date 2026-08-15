import type {
  EditorElementBase,
  EditorGroup,
  EditorNode,
  EditorRawNode,
  EditorShape,
  EditorText,
} from '../../types'
import { SvgParseError } from '../errors'
import type { SvgImportWarning } from '../types'
import { parseNumberAttribute } from './parseNumber'
import { parseShapeStyle, parseTextStyle } from './parseStyle'
import { parseTransform } from './parseTransform'

const commonAttributeNames = new Set([
  'data-editor-id',
  'data-name',
  'data-role',
  'data-visible',
  'display',
  'id',
  'opacity',
  'transform',
])

const attributeNamesByElement = {
  circle: new Set([
    'cx',
    'cy',
    'fill',
    'fill-opacity',
    'r',
    'stroke',
    'stroke-linecap',
    'stroke-linejoin',
    'stroke-opacity',
    'stroke-width',
  ]),
  ellipse: new Set([
    'cx',
    'cy',
    'fill',
    'fill-opacity',
    'rx',
    'ry',
    'stroke',
    'stroke-linecap',
    'stroke-linejoin',
    'stroke-opacity',
    'stroke-width',
  ]),
  g: new Set<string>(),
  line: new Set([
    'fill',
    'fill-opacity',
    'stroke',
    'stroke-linecap',
    'stroke-linejoin',
    'stroke-opacity',
    'stroke-width',
    'x1',
    'x2',
    'y1',
    'y2',
  ]),
  path: new Set([
    'd',
    'fill',
    'fill-opacity',
    'stroke',
    'stroke-linecap',
    'stroke-linejoin',
    'stroke-opacity',
    'stroke-width',
  ]),
  rect: new Set([
    'fill',
    'fill-opacity',
    'height',
    'rx',
    'ry',
    'stroke',
    'stroke-linecap',
    'stroke-linejoin',
    'stroke-opacity',
    'stroke-width',
    'width',
    'x',
    'y',
  ]),
  text: new Set([
    'fill',
    'font-family',
    'font-size',
    'font-style',
    'font-weight',
    'letter-spacing',
    'text-anchor',
    'x',
    'y',
  ]),
} as const

function parseBase(element: Element): EditorElementBase {
  const id =
    element.getAttribute('data-editor-id') ?? element.getAttribute('id')

  if (!id) {
    throw new SvgParseError(
      `SVG element <${element.localName}> must define a stable id`,
    )
  }

  const role = element.getAttribute('data-role')

  return {
    id,
    isLocked: false,
    isVisible:
      element.getAttribute('data-visible') !== 'false' &&
      element.getAttribute('display') !== 'none',
    name: element.getAttribute('data-name') ?? element.getAttribute('id') ?? id,
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

function createRawNode(element: Element): EditorRawNode {
  const id =
    element.getAttribute('data-editor-id') ??
    element.getAttribute('id') ??
    'unsupported'

  return {
    id,
    isLocked: true,
    isVisible: true,
    name: element.getAttribute('data-name') ?? element.getAttribute('id') ?? id,
    opacity: 1,
    rawSvg: new XMLSerializer().serializeToString(element),
    transform: { rotation: 0, scaleX: 1, scaleY: 1, x: 0, y: 0 },
    type: 'raw',
  }
}

function getUnsupportedReason(element: Element): string | null {
  if (!(element.localName in attributeNamesByElement)) {
    return `Unsupported SVG element: ${element.localName}`
  }

  if (element.localName === 'text' && element.children.length > 0) {
    return 'Text with nested SVG elements is not editable'
  }

  const supportedAttributes =
    attributeNamesByElement[
      element.localName as keyof typeof attributeNamesByElement
    ]

  for (const attribute of Array.from(element.attributes)) {
    if (
      !commonAttributeNames.has(attribute.name) &&
      !supportedAttributes.has(attribute.name)
    ) {
      return `Unsupported SVG attribute: ${attribute.name}`
    }
  }

  return null
}

function parseNode(element: Element, warnings: SvgImportWarning[]): EditorNode {
  const unsupportedReason = getUnsupportedReason(element)

  if (unsupportedReason) {
    return createLockedRawNode(element, unsupportedReason, warnings)
  }

  try {
    if (element.localName === 'g') {
      const group: EditorGroup = {
        ...parseBase(element),
        children: Array.from(element.children).map((child) =>
          parseNode(child, warnings),
        ),
        type: 'group',
      }

      return group
    }

    if (element.localName === 'text') {
      return parseText(element)
    }

    return parseShape(element)
  } catch (error) {
    if (!(error instanceof SvgParseError)) {
      throw error
    }

    return createLockedRawNode(element, error.message, warnings)
  }
}

function createLockedRawNode(
  element: Element,
  reason: string,
  warnings: SvgImportWarning[],
): EditorRawNode {
  const rawNode = createRawNode(element)

  warnings.push({
    code: 'locked-raw-node',
    editorId: rawNode.id,
    elementName: element.localName,
    reason,
  })

  return rawNode
}

export function parseLayersTolerant(
  root: Element,
  warnings: SvgImportWarning[],
): EditorNode[] {
  return Array.from(root.children)
    .filter((element) => element.localName !== 'defs')
    .map((element) => parseNode(element, warnings))
}
