import type { EditorDocument } from '@entities/editorDocument'
import { observer } from 'mobx-react-lite'

import { LayerNode } from './LayerNode'
import { SvgDefinitions } from './SvgDefinitions'

interface LayerRendererProps {
  document: EditorDocument
  onSelectNode: (nodeId: string) => void
}

export const LayerRenderer = observer(function LayerRenderer({
  document,
  onSelectNode,
}: LayerRendererProps) {
  return (
    <>
      <SvgDefinitions definitions={document.definitions} />
      {document.layers.map((node) => (
        <LayerNode key={node.id} node={node} onSelectNode={onSelectNode} />
      ))}
    </>
  )
})
