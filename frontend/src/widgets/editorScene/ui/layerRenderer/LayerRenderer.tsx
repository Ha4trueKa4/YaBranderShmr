import type { EditorDocument, EditorShape } from '@entities/editorDocument'
import { observer } from 'mobx-react-lite'
import type { PointerEvent as ReactPointerEvent, RefCallback } from 'react'

import { LayerNode } from './LayerNode'
import { SvgDefinitions } from './SvgDefinitions'

interface LayerRendererProps {
  document: EditorDocument
  onSelectNode: (nodeId: string) => void
  onShapePointerDown: (
    node: EditorShape,
    event: ReactPointerEvent<SVGGElement>,
  ) => void
  onShapePointerMove: (event: ReactPointerEvent<SVGGElement>) => void
  onShapePointerUp: (event: ReactPointerEvent<SVGGElement>) => void
  renderVersion: number
  registerShapeElement: (nodeId: string) => RefCallback<SVGGElement>
}

export const LayerRenderer = observer(function LayerRenderer({
  document,
  onSelectNode,
  onShapePointerDown,
  onShapePointerMove,
  onShapePointerUp,
  renderVersion: _renderVersion,
  registerShapeElement,
}: LayerRendererProps) {
  return (
    <>
      <SvgDefinitions definitions={document.definitions} />
      {document.layers.map((node) => (
        <LayerNode
          key={node.id}
          node={node}
          onSelectNode={onSelectNode}
          onShapePointerDown={onShapePointerDown}
          onShapePointerMove={onShapePointerMove}
          onShapePointerUp={onShapePointerUp}
          registerShapeElement={registerShapeElement}
        />
      ))}
    </>
  )
})
