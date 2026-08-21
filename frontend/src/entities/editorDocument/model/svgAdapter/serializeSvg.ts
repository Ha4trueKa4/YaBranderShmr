import type {
  EditorDefinitions,
  EditorDocument,
  EditorGroup,
  EditorNode,
  EditorShape,
  EditorText,
  Paint,
  ShapeStyle,
  TextStyle,
  Transform,
} from '../types'

type AttributeValue = number | string | undefined

function escapeAttribute(value: string): string {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&apos;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
}

function escapeText(value: string): string {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
}

function serializeAttributes(
  attributes: Record<string, AttributeValue>,
): string {
  return Object.entries(attributes)
    .filter(([, value]) => value !== undefined)
    .map(([name, value]) => ` ${name}="${escapeAttribute(String(value))}"`)
    .join('')
}

function serializePaint(paint: Paint): string {
  switch (paint.type) {
    case 'color':
      return paint.value
    case 'gradient':
      return `url(#${paint.gradientId})`
    case 'none':
      return 'none'
  }
}

function serializeTransform(transform: Transform): string | undefined {
  const operations: string[] = []

  if (transform.x !== 0 || transform.y !== 0) {
    operations.push(`translate(${transform.x} ${transform.y})`)
  }

  if (transform.rotation !== 0) {
    const center = transform.rotationCenter
    operations.push(
      center
        ? `rotate(${transform.rotation} ${center.x} ${center.y})`
        : `rotate(${transform.rotation})`,
    )
  }

  if (transform.scaleX !== 1 || transform.scaleY !== 1) {
    operations.push(`scale(${transform.scaleX} ${transform.scaleY})`)
  }

  return operations.length > 0 ? operations.join(' ') : undefined
}

function serializeBaseAttributes(
  node: EditorNode | EditorGroup | EditorShape | EditorText,
): Record<string, AttributeValue> {
  return {
    'data-editor-id': node.id,
    'data-name': node.name,
    'data-role': node.role,
    'data-visible': node.isVisible ? undefined : 'false',
    display: node.isVisible ? undefined : 'none',
    id: node.id,
    opacity: node.opacity,
    transform: serializeTransform(node.transform),
  }
}

function serializeShapeStyle(
  style: ShapeStyle,
): Record<string, AttributeValue> {
  return {
    fill: serializePaint(style.fill),
    'fill-opacity': style.fillOpacity,
    stroke: serializePaint(style.stroke),
    'stroke-dasharray': style.strokeDasharray,
    'stroke-linecap': style.strokeLineCap,
    'stroke-linejoin': style.strokeLineJoin,
    'stroke-opacity': style.strokeOpacity,
    'stroke-width': style.strokeWidth,
  }
}

function serializeTextStyle(style: TextStyle): Record<string, AttributeValue> {
  return {
    fill: serializePaint(style.fill),
    'font-family': style.fontFamily,
    'font-size': style.fontSize,
    'font-style': style.fontStyle,
    'font-weight': style.fontWeight,
    'letter-spacing': style.letterSpacing,
    'text-anchor': style.textAnchor,
  }
}

function serializeTextContent(node: EditorText): string {
  const lines = node.text.split('\n')

  if (lines.length === 1) {
    return escapeText(node.text)
  }

  const lineHeight = node.style.lineHeight ?? node.style.fontSize * 1.2

  return lines
    .map(
      (line, index) =>
        `<tspan${serializeAttributes({
          dy: index === 0 ? 0 : lineHeight,
          x: node.x,
        })}>${escapeText(line)}</tspan>`,
    )
    .join('')
}

function serializeShape(node: EditorShape): string {
  const { type: _type, ...geometry } = node.geometry
  const attributes = {
    ...serializeBaseAttributes(node),
    ...serializeShapeStyle(node.style),
    ...geometry,
  }

  return `<${node.shapeType}${serializeAttributes(attributes)} />`
}

function serializeText(node: EditorText): string {
  const attributes = {
    ...serializeBaseAttributes(node),
    ...serializeTextStyle(node.style),
    x: node.x,
    y: node.y,
  }

  return `<text${serializeAttributes(attributes)}>${serializeTextContent(node)}</text>`
}

function serializeGroup(node: EditorGroup): string {
  return `<g${serializeAttributes(serializeBaseAttributes(node))}>${node.children.map(serializeNode).join('')}</g>`
}

function serializeNode(node: EditorNode): string {
  if (node.type === 'raw') {
    return node.rawSvg
  }

  if (node.type === 'group') {
    return serializeGroup(node)
  }

  if (node.type === 'text') {
    return serializeText(node)
  }

  return serializeShape(node)
}

function serializeDefinitions(
  definitions: EditorDefinitions | undefined,
): string {
  if (!definitions) {
    return ''
  }

  const gradients = definitions.gradients
    .map((gradient) => {
      const attributes = {
        ...gradient.attributes,
        id: gradient.id,
      }
      const stops = gradient.stops
        .map(
          (stop) =>
            `<stop${serializeAttributes({
              offset: stop.offset,
              'stop-color': stop.color,
              'stop-opacity': stop.opacity,
            })} />`,
        )
        .join('')

      return `<${gradient.type}${serializeAttributes(attributes)}>${stops}</${gradient.type}>`
    })
    .join('')

  if (!definitions.rawSvg && !gradients) {
    return ''
  }

  return `<defs>${definitions.rawSvg ?? ''}${gradients}</defs>`
}

export function serializeSvg(document: EditorDocument): string {
  const rootAttributes = serializeAttributes({
    height: document.height,
    viewBox: `${document.viewBoxX ?? 0} ${document.viewBoxY ?? 0} ${document.width} ${document.height}`,
    width: document.width,
    xmlns: 'http://www.w3.org/2000/svg',
  })

  return `<svg${rootAttributes}>${serializeDefinitions(document.definitions)}${document.layers.map(serializeNode).join('')}</svg>`
}
