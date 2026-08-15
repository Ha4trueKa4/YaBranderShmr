import type { EditorNode } from '@entities/editorDocument'
import { ChevronRight, Eye, EyeOff } from 'lucide-react'
import { observer } from 'mobx-react-lite'

import { getLayerPresentation } from '../layerPresentation'
import styles from './LayersPanel.module.css'

interface LayerTreeItemProps {
  depth: number
  expandedGroupIds: ReadonlySet<string>
  node: EditorNode
  onSelectNode: (nodeId: string) => void
  onToggleGroup: (groupId: string) => void
  onToggleVisibility: (nodeId: string) => void
  selectedNodeId: string | null
}

export const LayerTreeItem = observer(function LayerTreeItem({
  depth,
  expandedGroupIds,
  node,
  onSelectNode,
  onToggleGroup,
  onToggleVisibility,
  selectedNodeId,
}: LayerTreeItemProps) {
  const isGroup = node.type === 'group'
  const hasChildren = isGroup && node.children.length > 0
  const isExpanded = isGroup && expandedGroupIds.has(node.id)
  const isSelected = node.id === selectedNodeId
  const presentation = getLayerPresentation(node)
  const LayerIcon = presentation.icon
  const VisibilityIcon = node.isVisible ? Eye : EyeOff
  const indentation = `calc(var(--space-4) + ${depth} * var(--space-5))`

  return (
    <div className={styles.treeItem}>
      <div
        className={`${isSelected ? styles.selected : styles.layer} ${
          isExpanded ? styles.groupExpanded : ''
        } ${node.isVisible ? '' : styles.hidden}`}
        style={{
          paddingInlineStart: indentation,
        }}
      >
        <button
          aria-label={`Select layer ${node.name}`}
          className={styles.selectRowButton}
          onClick={() => onSelectNode(node.id)}
          type="button"
        />
        {isGroup ? (
          <button
            className={styles.chevronButton}
            disabled={!hasChildren}
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
        <div className={styles.itemButton}>
          <LayerIcon className={styles.icon} size={16} strokeWidth={2} />
          <span className={styles.layerName}>{node.name}</span>
        </div>
        <button
          className={styles.visibilityButton}
          onClick={() => onToggleVisibility(node.id)}
          type="button"
        >
          <VisibilityIcon size={16} strokeWidth={2} />
        </button>
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
              onToggleVisibility={onToggleVisibility}
              selectedNodeId={selectedNodeId}
            />
          ))}
        </div>
      )}
    </div>
  )
})
