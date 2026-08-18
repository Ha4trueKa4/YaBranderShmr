import type {
  EditorNode,
  EditorShape,
  EditorText,
} from '@entities/editorDocument'
import { observer } from 'mobx-react-lite'
import type { PointerEvent as ReactPointerEvent, RefCallback } from 'react'

import { getTransformValue } from './model/getTransformValue'
import { ShapeLayer } from './ShapeLayer'
import { TextLayer } from './TextLayer'

interface LayerNodeProps {
  canMoveLayers: boolean
  node: EditorNode
  onSelectNode: (nodeId: string) => void
  onShapePointerDown: (
    node: EditorShape,
    event: ReactPointerEvent<SVGGElement>,
  ) => void
  onShapePointerMove: (event: ReactPointerEvent<SVGGElement>) => void
  onShapePointerUp: (event: ReactPointerEvent<SVGGElement>) => void
  onTextPointerDown: (
    node: EditorText,
    event: ReactPointerEvent<SVGGElement>,
  ) => void
  registerEditableElement: (nodeId: string) => RefCallback<SVGGElement>
}

export const LayerNode = observer(function LayerNode({
  canMoveLayers,
  node,
  onSelectNode,
  onShapePointerDown,
  onShapePointerMove,
  onShapePointerUp,
  onTextPointerDown,
  registerEditableElement,
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
            canMoveLayers={canMoveLayers}
            node={child}
            onSelectNode={onSelectNode}
            onShapePointerDown={onShapePointerDown}
            onShapePointerMove={onShapePointerMove}
            onShapePointerUp={onShapePointerUp}
            onTextPointerDown={onTextPointerDown}
            registerEditableElement={registerEditableElement}
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
    return (
      <g
        data-editor-node-id={node.id}
        onPointerCancel={onShapePointerUp}
        onPointerDown={(event) => onTextPointerDown(node, event)}
        onPointerMove={onShapePointerMove}
        onPointerUp={onShapePointerUp}
        ref={registerEditableElement(node.id)}
        style={{
          cursor: node.isLocked
            ? 'not-allowed'
            : canMoveLayers
              ? 'grab'
              : 'pointer',
        }}
      >
        <TextLayer node={node} />
      </g>
    )
  }

  return (
    <g
      data-editor-node-id={node.id}
      onPointerCancel={onShapePointerUp}
      onPointerDown={(event) => onShapePointerDown(node, event)}
      onPointerMove={onShapePointerMove}
      onPointerUp={onShapePointerUp}
      ref={registerEditableElement(node.id)}
      style={{
        cursor: node.isLocked
          ? 'not-allowed'
          : canMoveLayers
            ? 'grab'
            : 'pointer',
      }}
    >
      <ShapeLayer node={node} />
    </g>
  )
})
