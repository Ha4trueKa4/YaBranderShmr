import type {
  EditorDocument,
  EditorRawNode,
  EditorShape,
  EditorText,
} from '@entities/editorDocument'
import { observer } from 'mobx-react-lite'
import type { PointerEvent as ReactPointerEvent, RefCallback } from 'react'

import { LayerNode } from './LayerNode'
import { SvgDefinitions } from './SvgDefinitions'

interface LayerRendererProps {
  canMoveLayers: boolean
  document: EditorDocument
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
  renderVersion: number
  registerEditableElement: (nodeId: string) => RefCallback<SVGGElement>
}

export const LayerRenderer = observer(function LayerRenderer({
  canMoveLayers,
  document,
  onSelectNode,
  onRawPointerDown,
  onShapePointerDown,
  onShapePointerMove,
  onShapePointerUp,
  onTextPointerDown,
  renderVersion: _renderVersion,
  registerEditableElement,
}: LayerRendererProps) {
  return (
    <>
      <SvgDefinitions definitions={document.definitions} />
      {document.layers.map((node) => (
        <LayerNode
          key={node.id}
          canMoveLayers={canMoveLayers}
          node={node}
          onSelectNode={onSelectNode}
          onRawPointerDown={onRawPointerDown}
          onShapePointerDown={onShapePointerDown}
          onShapePointerMove={onShapePointerMove}
          onShapePointerUp={onShapePointerUp}
          onTextPointerDown={onTextPointerDown}
          registerEditableElement={registerEditableElement}
        />
      ))}
    </>
  )
})
