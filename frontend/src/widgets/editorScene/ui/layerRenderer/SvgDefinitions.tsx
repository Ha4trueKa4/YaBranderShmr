import type { EditorDefinitions } from '@entities/editorDocument'

interface SvgDefinitionsProps {
  definitions: EditorDefinitions | undefined
}

export function SvgDefinitions({ definitions }: SvgDefinitionsProps) {
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
