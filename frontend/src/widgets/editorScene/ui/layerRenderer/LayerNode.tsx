import type {
  EditorNode,
  EditorRawNode,
  EditorShape,
  EditorText,
} from '@entities/editorDocument'
import { observer } from 'mobx-react-lite'
import type { PointerEvent as ReactPointerEvent, RefCallback } from 'react'

import { getPaintValue } from './model/getPaintValue'
import { getTransformValue } from './model/getTransformValue'
import { ShapeLayer } from './ShapeLayer'
import { TextLayer } from './TextLayer'

interface LayerNodeProps {
  canMoveLayers: boolean
  node: EditorNode
  onSelectNode: (nodeId: string, isMultiSelect: boolean) => void
  onRawPointerDown: (
    node: EditorRawNode,
    event: ReactPointerEvent<SVGGElement>,
  ) => void
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
  onRawPointerDown,
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
      <g
        fill={node.fill ? getPaintValue(node.fill) : undefined}
        opacity={node.opacity}
        transform={getTransformValue(node.transform)}
      >
        {node.children.map((child) => (
          <LayerNode
            key={child.id}
            canMoveLayers={canMoveLayers}
            node={child}
            onSelectNode={onSelectNode}
            onRawPointerDown={onRawPointerDown}
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
    return (
      <g
        data-editor-node-id={node.id}
        onPointerCancel={onShapePointerUp}
        onPointerDown={(event) => onRawPointerDown(node, event)}
        onPointerMove={onShapePointerMove}
        onPointerUp={onShapePointerUp}
        ref={registerEditableElement(node.id)}
        style={{ cursor: canMoveLayers ? 'default' : 'inherit' }}
        transform={getTransformValue(node.transform)}
      >
        {/* biome-ignore lint/security/noDangerouslySetInnerHtml: raw SVG is sanitized before import. */}
        <g dangerouslySetInnerHTML={{ __html: node.rawSvg }} />
      </g>
    )
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
          cursor: canMoveLayers
            ? node.isLocked
              ? 'not-allowed'
              : 'default'
            : 'inherit',
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
        cursor: canMoveLayers
          ? node.isLocked
            ? 'not-allowed'
            : 'default'
          : 'inherit',
      }}
    >
      <ShapeLayer node={node} />
    </g>
  )
})
