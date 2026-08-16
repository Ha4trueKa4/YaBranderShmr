import type { EditorText } from '@entities/editorDocument'
import { observer } from 'mobx-react-lite'

import { getPaintValue } from './model/getPaintValue'
import { getTransformValue } from './model/getTransformValue'

interface TextLayerProps {
  node: EditorText
}

function getLines(text: string, lineHeight: number) {
  const occurrences = new Map<string, number>()
  let isFirstLine = true

  return text.split('\n').map((content) => {
    const occurrence = (occurrences.get(content) ?? 0) + 1

    occurrences.set(content, occurrence)

    const line = {
      content,
      dy: isFirstLine ? 0 : lineHeight,
      key: `${content}-${occurrence}`,
    }

    isFirstLine = false

    return line
  })
}

export const TextLayer = observer(function TextLayer({ node }: TextLayerProps) {
  const lines = getLines(
    node.text,
    node.style.lineHeight ?? node.style.fontSize * 1.2,
  )

  return (
    <text
      fill={getPaintValue(node.style.fill)}
      fontFamily={node.style.fontFamily}
      fontSize={node.style.fontSize}
      fontStyle={node.style.fontStyle}
      fontWeight={node.style.fontWeight}
      letterSpacing={node.style.letterSpacing}
      opacity={node.opacity}
      textAnchor={node.style.textAnchor}
      transform={getTransformValue(node.transform)}
      x={node.x}
      y={node.y}
    >
      {lines.length === 1
        ? node.text
        : lines.map((line) => (
            <tspan dy={line.dy} key={line.key} x={node.x}>
              {line.content}
            </tspan>
          ))}
    </text>
  )
})
