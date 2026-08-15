import type { EditorNode } from '@entities/editorDocument'
import { ChevronRight, Eye } from 'lucide-react'
import { observer } from 'mobx-react-lite'

import { getLayerPresentation } from '../layerPresentation'
import styles from './LayersPanel.module.css'

interface LayerTreeItemProps {
  depth: number
  expandedGroupIds: ReadonlySet<string>
  node: EditorNode
  onSelectNode: (nodeId: string) => void
  onToggleGroup: (groupId: string) => void
  selectedNodeId: string | null
}

export const LayerTreeItem = observer(function LayerTreeItem({
  depth,
  expandedGroupIds,
  node,
  onSelectNode,
  onToggleGroup,
  selectedNodeId,
}: LayerTreeItemProps) {
  const isGroup = node.type === 'group'
  const hasChildren = isGroup && node.children.length > 0
  const isExpanded = isGroup && expandedGroupIds.has(node.id)
  const isSelected = node.id === selectedNodeId
  const presentation = getLayerPresentation(node)
  const LayerIcon = presentation.icon
  const indentation = `calc(var(--space-4) + ${depth} * var(--space-5))`

  return (
    <div className={styles.treeItem}>
      <div
        className={isSelected ? styles.selected : styles.layer}
        style={{
          paddingInlineStart: indentation,
        }}
      >
        {hasChildren ? (
          <button
            className={styles.chevronButton}
            onClick={() => onToggleGroup(node.id)}
            type="button"
          >
            <ChevronRight
              className={isExpanded ? styles.chevronExpanded : undefined}
              size={16}
              strokeWidth={2}
            />
          </button>
        ) : (
          <span className={styles.chevronPlaceholder} />
        )}
        <button
          className={styles.itemButton}
          onClick={() => onSelectNode(node.id)}
          type="button"
        >
          <LayerIcon className={styles.icon} size={16} strokeWidth={2} />
          <span className={styles.layerName}>{node.name}</span>
        </button>
        <Eye className={styles.visibilityIcon} size={16} strokeWidth={2} />
      </div>
      {isGroup && isExpanded && (
        <div className={styles.children}>
          {node.children.map((child: EditorNode) => (
            <LayerTreeItem
              depth={depth + 1}
              expandedGroupIds={expandedGroupIds}
              key={child.id}
              node={child}
              onSelectNode={onSelectNode}
              onToggleGroup={onToggleGroup}
              selectedNodeId={selectedNodeId}
            />
          ))}
        </div>
      )}
    </div>
  )
})
