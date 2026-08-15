import type { EditorNode } from '@entities/editorDocument'

import { getTransformValue } from './model/getTransformValue'
import { ShapeLayer } from './ShapeLayer'
import { TextLayer } from './TextLayer'

interface LayerNodeProps {
  node: EditorNode
}

export function LayerNode({ node }: LayerNodeProps) {
  if (!node.isVisible) {
    return null
  }

  if (node.type === 'group') {
    return (
      <g opacity={node.opacity} transform={getTransformValue(node.transform)}>
        {node.children.map((child) => (
          <LayerNode key={child.id} node={child} />
        ))}
      </g>
    )
  }

  if (node.type === 'text') {
    return <TextLayer node={node} />
  }

  return (
    <g>
      <ShapeLayer node={node} />
    </g>
  )
}
