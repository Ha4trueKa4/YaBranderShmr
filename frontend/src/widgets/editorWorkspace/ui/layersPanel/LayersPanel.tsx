import type { EditorNode } from '@entities/editorDocument'
import { observer } from 'mobx-react-lite'

import plus from './assets/plus.svg'
import styles from './LayersPanel.module.css'
import { LayerTreeItem } from './LayerTreeItem'

interface LayersPanelProps {
  expandedGroupIds: ReadonlySet<string>
  nodes: EditorNode[]
  onSelectNode: (nodeId: string) => void
  onToggleGroup: (groupId: string) => void
  selectedNodeId: string | null
}

export const LayersPanel = observer(function LayersPanel({
  expandedGroupIds,
  nodes,
  onSelectNode,
  onToggleGroup,
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
            expandedGroupIds={expandedGroupIds}
            key={node.id}
            node={node}
            onSelectNode={onSelectNode}
            onToggleGroup={onToggleGroup}
            selectedNodeId={selectedNodeId}
          />
        ))}
      </div>
    </section>
  )
})
