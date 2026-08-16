import type { EditorNode, EditorShape } from '@entities/editorDocument'
import { observer } from 'mobx-react-lite'
import type { PointerEvent as ReactPointerEvent, RefCallback } from 'react'

import { getTransformValue } from './model/getTransformValue'
import { ShapeLayer } from './ShapeLayer'
import { TextLayer } from './TextLayer'

interface LayerNodeProps {
  node: EditorNode
  onSelectNode: (nodeId: string) => void
  onShapePointerDown: (
    node: EditorShape,
    event: ReactPointerEvent<SVGGElement>,
  ) => void
  onShapePointerMove: (event: ReactPointerEvent<SVGGElement>) => void
  onShapePointerUp: (event: ReactPointerEvent<SVGGElement>) => void
  registerShapeElement: (nodeId: string) => RefCallback<SVGGElement>
}

export const LayerNode = observer(function LayerNode({
  node,
  onSelectNode,
  onShapePointerDown,
  onShapePointerMove,
  onShapePointerUp,
  registerShapeElement,
}: LayerNodeProps) {
  if (!node.isVisible) {
    return null
  }

  if (node.type === 'group') {
    return (
      <g opacity={node.opacity} transform={getTransformValue(node.transform)}>
        {node.children.map((child) => (
          <LayerNode
            key={child.id}
            node={child}
            onSelectNode={onSelectNode}
            onShapePointerDown={onShapePointerDown}
            onShapePointerMove={onShapePointerMove}
            onShapePointerUp={onShapePointerUp}
            registerShapeElement={registerShapeElement}
          />
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
    <g
      data-editor-node-id={node.id}
      onPointerCancel={onShapePointerUp}
      onPointerDown={(event) => onShapePointerDown(node, event)}
      onPointerMove={onShapePointerMove}
      onPointerUp={onShapePointerUp}
      ref={registerShapeElement(node.id)}
      style={{ cursor: node.isLocked ? 'not-allowed' : 'grab' }}
    >
      <ShapeLayer node={node} />
    </g>
  )
})
