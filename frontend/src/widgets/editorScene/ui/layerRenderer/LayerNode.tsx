import type { EditorNode } from '@entities/editorDocument'

import { getTransformValue } from './model/getTransformValue'
import { ShapeLayer } from './ShapeLayer'
import { TextLayer } from './TextLayer'

interface LayerNodeProps {
  node: EditorNode
  onSelectNode: (nodeId: string) => void
}

export function LayerNode({ node, onSelectNode }: LayerNodeProps) {
  if (!node.isVisible) {
    return null
  }

  if (node.type === 'group') {
    return (
      <g opacity={node.opacity} transform={getTransformValue(node.transform)}>
        {node.children.map((child) => (
          <LayerNode key={child.id} node={child} onSelectNode={onSelectNode} />
        ))}
      </g>
    )
  }

  if (node.type === 'raw') {
    // biome-ignore lint/security/noDangerouslySetInnerHtml: raw SVG is sanitized before import.
    return <g dangerouslySetInnerHTML={{ __html: node.rawSvg }} />
  }

  if (node.type === 'text') {
    return <TextLayer node={node} onSelect={() => onSelectNode(node.id)} />
  }

  return (
    // biome-ignore lint/a11y/noStaticElementInteractions: SVG canvas elements are selected with a pointer.
    <g onClick={() => onSelectNode(node.id)} style={{ cursor: 'pointer' }}>
      <ShapeLayer node={node} />
    </g>
  )
}
