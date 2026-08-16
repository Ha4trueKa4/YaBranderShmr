import type { EditorText } from '@entities/editorDocument'

import { getPaintValue } from './model/getPaintValue'
import { getTransformValue } from './model/getTransformValue'

interface TextLayerProps {
  node: EditorText
  onSelect: () => void
}

export function TextLayer({ node, onSelect }: TextLayerProps) {
  return (
    // biome-ignore lint/a11y/noStaticElementInteractions: SVG canvas elements are selected with a pointer.
    <text
      fill={getPaintValue(node.style.fill)}
      fontFamily={node.style.fontFamily}
      fontSize={node.style.fontSize}
      fontStyle={node.style.fontStyle}
      fontWeight={node.style.fontWeight}
      letterSpacing={node.style.letterSpacing}
      opacity={node.opacity}
      onClick={onSelect}
      style={{ cursor: 'pointer' }}
      textAnchor={node.style.textAnchor}
      transform={getTransformValue(node.transform)}
      x={node.x}
      y={node.y}
    >
      {node.text}
    </text>
  )
}
