import type { EditorNode } from '@entities/editorDocument'
import { observer } from 'mobx-react-lite'
import { useTranslation } from 'react-i18next'

import plus from './assets/plus.svg'
import styles from './LayersPanel.module.css'
import { LayerTreeItem } from './LayerTreeItem'

interface LayersPanelProps {
  expandedGroupIds: ReadonlySet<string>
  nodes: EditorNode[]
  onSelectNode: (nodeId: string, isMultiSelect: boolean) => void
  onToggleGroup: (groupId: string) => void
  onToggleVisibility: (nodeId: string) => void
  selectedNodeIds: ReadonlySet<string>
}

export const LayersPanel = observer(function LayersPanel({
  expandedGroupIds,
  nodes,
  onSelectNode,
  onToggleGroup,
  onToggleVisibility,
  selectedNodeIds,
}: LayersPanelProps) {
  const { t } = useTranslation()
  return (
    <section className={styles.panel}>
      <div className={styles.header}>
        <h2>{t('Layers')}</h2>
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
            onToggleVisibility={onToggleVisibility}
            selectedNodeIds={selectedNodeIds}
          />
        ))}
      </div>
    </section>
  )
})
