import type { EditorText } from '@entities/editorDocument'

import { getPaintValue } from './model/getPaintValue'
import { getTransformValue } from './model/getTransformValue'

interface TextLayerProps {
  node: EditorText
}

export function TextLayer({ node }: TextLayerProps) {
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
      {node.text}
    </text>
  )
}
