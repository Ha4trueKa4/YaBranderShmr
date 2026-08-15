import type {
  EditorDefinitions,
  EditorDocument,
  EditorNode,
  EditorShape,
  Paint,
  Transform,
} from '@entities/editorDocument'

interface LayerRendererProps {
  document: EditorDocument
}

function getPaintValue(paint: Paint): string {
  switch (paint.type) {
    case 'color':
      return paint.value
    case 'gradient':
      return `url(#${paint.gradientId})`
    case 'none':
      return 'none'
  }
}

function getTransformValue(transform: Transform): string | undefined {
  const operations: string[] = []

  if (transform.x !== 0 || transform.y !== 0) {
    operations.push(`translate(${transform.x} ${transform.y})`)
  }

  if (transform.rotation !== 0) {
    const rotationCenter = transform.rotationCenter
    const rotation = rotationCenter
      ? `${transform.rotation} ${rotationCenter.x} ${rotationCenter.y}`
      : String(transform.rotation)

    operations.push(`rotate(${rotation})`)
  }

  if (transform.scaleX !== 1 || transform.scaleY !== 1) {
    operations.push(`scale(${transform.scaleX} ${transform.scaleY})`)
  }

  return operations.length > 0 ? operations.join(' ') : undefined
}

function renderDefinitions(definitions: EditorDefinitions | undefined) {
  if (!definitions || definitions.gradients.length === 0) {
    return null
  }

  return (
    <defs>
      {definitions.gradients.map((gradient) => {
        const stops = gradient.stops.map((stop) => (
          <stop
            key={stop.offset}
            offset={stop.offset}
            stopColor={stop.color}
            stopOpacity={stop.opacity}
          />
        ))

        if (gradient.type === 'linearGradient') {
          return (
            <linearGradient
              id={gradient.id}
              key={gradient.id}
              x1={gradient.attributes.x1}
              x2={gradient.attributes.x2}
              y1={gradient.attributes.y1}
              y2={gradient.attributes.y2}
            >
              {stops}
            </linearGradient>
          )
        }

        return (
          <radialGradient
            cx={gradient.attributes.cx}
            cy={gradient.attributes.cy}
            id={gradient.id}
            key={gradient.id}
            r={gradient.attributes.r}
          >
            {stops}
          </radialGradient>
        )
      })}
    </defs>
  )
}

function renderShape(node: EditorShape) {
  const commonProps = {
    fill: getPaintValue(node.style.fill),
    fillOpacity: node.style.fillOpacity,
    opacity: node.opacity,
    stroke: getPaintValue(node.style.stroke),
    strokeLinecap: node.style.strokeLineCap,
    strokeLinejoin: node.style.strokeLineJoin,
    strokeOpacity: node.style.strokeOpacity,
    strokeWidth: node.style.strokeWidth,
    transform: getTransformValue(node.transform),
  }

  switch (node.shapeType) {
    case 'rect':
      return <rect {...commonProps} {...node.geometry} />
    case 'circle':
      return <circle {...commonProps} {...node.geometry} />
    case 'ellipse':
      return <ellipse {...commonProps} {...node.geometry} />
    case 'line':
      return <line {...commonProps} {...node.geometry} />
    case 'path':
      return <path {...commonProps} {...node.geometry} />
  }
}

function renderNode(node: EditorNode) {
  if (!node.isVisible) {
    return null
  }

  if (node.type === 'group') {
    return (
      <g
        key={node.id}
        opacity={node.opacity}
        transform={getTransformValue(node.transform)}
      >
        {node.children.map(renderNode)}
      </g>
    )
  }

  if (node.type === 'text') {
    return (
      <text
        fill={getPaintValue(node.style.fill)}
        fontFamily={node.style.fontFamily}
        fontSize={node.style.fontSize}
        fontStyle={node.style.fontStyle}
        fontWeight={node.style.fontWeight}
        key={node.id}
        letterSpacing={node.style.letterSpacing}
        opacity={node.opacity}
        textAnchor={node.style.textAnchor}
        transform={getTransformValue(node.transform)}
        x={node.x}
        y={node.y}
      >
        {node.text}
      </text>
    )
  }

  return <g key={node.id}>{renderShape(node)}</g>
}

export function LayerRenderer({ document }: LayerRendererProps) {
  return (
    <>
      {renderDefinitions(document.definitions)}
      {document.layers.map(renderNode)}
    </>
  )
}
