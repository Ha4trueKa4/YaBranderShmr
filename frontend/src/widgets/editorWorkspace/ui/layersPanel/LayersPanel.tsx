import type { EditorNode } from '@entities/editorDocument'
import { observer } from 'mobx-react-lite'

import plus from './assets/plus.svg'
import styles from './LayersPanel.module.css'
import { LayerTreeItem } from './LayerTreeItem'

interface LayersPanelProps {
  nodes: EditorNode[]
  onSelectNode: (nodeId: string) => void
  selectedNodeId: string | null
}

export const LayersPanel = observer(function LayersPanel({
  nodes,
  onSelectNode,
  selectedNodeId,
}: LayersPanelProps) {
  return (
    <section className={styles.panel}>
      <div className={styles.header}>
        <h2>Layers</h2>
        {/* TODO: открыть feature добавления нового слоя. */}
        <button className={styles.addButton} type="button">
          <img alt="" src={plus} />
        </button>
      </div>
      <div className={styles.layerList}>
        {nodes.map((node) => (
          <LayerTreeItem
            depth={0}
            key={node.id}
            node={node}
            onSelectNode={onSelectNode}
            selectedNodeId={selectedNodeId}
          />
        ))}
      </div>
    </section>
  )
})
