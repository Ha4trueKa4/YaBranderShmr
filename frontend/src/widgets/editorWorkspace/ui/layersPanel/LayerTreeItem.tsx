import type { EditorNode } from '@entities/editorDocument'
import type { LucideIcon } from 'lucide-react'
import { Eye, Feather, Folder, Type } from 'lucide-react'

import chevronRight from './assets/chevron-right.svg'
import styles from './LayersPanel.module.css'

interface LayerTreeItemProps {
  depth: number
  expandedGroupIds: Set<string>
  node: EditorNode
  onSelectLayer: (layerId: string) => void
  onToggleGroup: (groupId: string) => void
  selectedLayerId: string | null
}

const iconByType: Record<EditorNode['type'], LucideIcon> = {
  group: Folder,
  shape: Feather,
  text: Type,
}

export function LayerTreeItem({
  depth,
  expandedGroupIds,
  node,
  onSelectLayer,
  onToggleGroup,
  selectedLayerId,
}: LayerTreeItemProps) {
  const isGroup = node.type === 'group'
  const isExpanded = isGroup && expandedGroupIds.has(node.id)
  const isSelected = node.id === selectedLayerId
  const LayerIcon = iconByType[node.type]

  return (
    <div className={styles.treeItem}>
      <div
        className={isSelected ? styles.selected : styles.layer}
        style={{
          paddingInlineStart: `calc(var(--space-4) + ${depth} * var(--space-5))`,
        }}
      >
        {isGroup ? (
          <button
            className={styles.chevronButton}
            onClick={() => onToggleGroup(node.id)}
            type="button"
          >
            <img
              alt=""
              className={isExpanded ? styles.chevronExpanded : styles.icon}
              src={chevronRight}
            />
          </button>
        ) : (
          <span className={styles.chevronPlaceholder} />
        )}
        <button
          className={styles.itemButton}
          onClick={() =>
            isGroup ? onToggleGroup(node.id) : onSelectLayer(node.id)
          }
          type="button"
        >
          <LayerIcon className={styles.icon} size={16} strokeWidth={2} />
          {node.name}
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
              onSelectLayer={onSelectLayer}
              onToggleGroup={onToggleGroup}
              selectedLayerId={selectedLayerId}
            />
          ))}
        </div>
      )}
    </div>
  )
}
