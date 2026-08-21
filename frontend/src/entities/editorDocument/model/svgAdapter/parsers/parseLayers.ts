import type {
  EditorElementBase,
  EditorGroup,
  EditorNode,
  EditorRawNode,
  EditorShape,
  EditorText,
  TextStyle,
} from '../../types'
import { SvgParseError } from '../errors'
import type { SvgImportWarning } from '../types'
import { parseNumberAttribute } from './parseNumber'
import { parseShapeStyle, parseTextStyle } from './parseStyle'
import { parseTransform } from './parseTransform'

const commonAttributeNames = new Set([
  'data-editor-layer',
  'data-editor-id',
  'data-name',
  'data-role',
  'data-visible',
  'display',
  'id',
  'opacity',
  'transform',
  'xmlns',
])

const attributeNamesByElement = {
  circle: new Set([
    'cx',
    'cy',
    'fill',
    'fill-opacity',
    'r',
    'stroke',
    'stroke-dasharray',
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
    'stroke-dasharray',
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
    'stroke-dasharray',
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
    'stroke-dasharray',
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
    'stroke-dasharray',
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

const tspanAttributeNames = new Set(['dy', 'x'])

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
  const tspans = Array.from(element.children)
  const style = parseTextStyle(element)

  return {
    ...parseBase(element),
    style: {
      ...style,
      ...(getTextLineHeight(tspans, style) ?? {}),
    },
    text:
      tspans.length > 0
        ? tspans.map((tspan) => tspan.textContent ?? '').join('\n')
        : (element.textContent ?? ''),
    type: 'text',
    x: parseNumberAttribute(element, 'x', 0),
    y: parseNumberAttribute(element, 'y', 0),
  }
}

function getTextLineHeight(
  tspans: Element[],
  style: TextStyle,
): Pick<TextStyle, 'lineHeight'> | undefined {
  const lineTspan = tspans.slice(1).find((tspan) => tspan.hasAttribute('dy'))

  if (!lineTspan) {
    return undefined
  }

  const lineHeight = parseNumberAttribute(lineTspan, 'dy')

  if (lineHeight <= 0) {
    throw new SvgParseError('SVG <tspan> dy must be greater than zero')
  }

  return { lineHeight: lineHeight || style.fontSize * 1.2 }
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

  if (element.localName === 'text' && !hasSupportedTextContent(element)) {
    return 'Text with unsupported nested SVG elements is not editable'
  }

  if (element.localName === 'text' && !hasLosslessTspanPositions(element)) {
    return 'Text with individually positioned lines is not editable'
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

  if (element.localName === 'text') {
    for (const tspan of Array.from(element.children)) {
      for (const attribute of Array.from(tspan.attributes)) {
        if (!tspanAttributeNames.has(attribute.name)) {
          return `Unsupported SVG attribute on <tspan>: ${attribute.name}`
        }
      }
    }
  }

  return null
}

function hasSupportedTextContent(element: Element): boolean {
  if (element.localName !== 'text' || element.children.length === 0) {
    return true
  }

  return (
    Array.from(element.children).every(
      (child) => child.localName === 'tspan',
    ) &&
    Array.from(element.childNodes).every(
      (child) =>
        child.nodeType !== Node.TEXT_NODE || !child.textContent?.trim(),
    )
  )
}

function hasLosslessTspanPositions(element: Element): boolean {
  if (element.localName !== 'text' || element.children.length === 0) {
    return true
  }

  const textX = parseNumberAttribute(element, 'x', 0)
  const tspans = Array.from(element.children)
  let lineHeight: number | null = null

  return tspans.every((tspan, index) => {
    const x = tspan.getAttribute('x')
    const dy = tspan.getAttribute('dy')

    if (x === null || Number(x) !== textX) {
      return false
    }

    if (index === 0) {
      return dy === null || Number(dy) === 0
    }

    if (dy === null || !Number.isFinite(Number(dy)) || Number(dy) <= 0) {
      return false
    }

    lineHeight ??= Number(dy)

    return Number(dy) === lineHeight
  })
}

function parseNode(element: Element, warnings: SvgImportWarning[]): EditorNode {
  try {
    const unsupportedReason = getUnsupportedReason(element)

    if (unsupportedReason) {
      return createLockedRawNode(element, unsupportedReason, warnings)
    }

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
    return createLockedRawNode(element, getParseFailureReason(error), warnings)
  }
}

function getParseFailureReason(error: unknown): string {
  if (error instanceof SvgParseError) {
    return error.message
  }

  return 'Unable to parse SVG node'
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
