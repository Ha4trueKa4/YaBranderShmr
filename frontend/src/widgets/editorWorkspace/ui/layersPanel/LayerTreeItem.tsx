import type { EditorNode } from '@entities/editorDocument'
import type { LucideIcon } from 'lucide-react'
import { Eye, Feather, Folder, Square, Type } from 'lucide-react'

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
  background: Square,
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
      <button
        className={isSelected ? styles.selected : styles.layer}
        onClick={() =>
          isGroup ? onToggleGroup(node.id) : onSelectLayer(node.id)
        }
        style={{
          paddingInlineStart: `calc(var(--space-4) + ${depth} * var(--space-5))`,
        }}
        type="button"
      >
        {isGroup ? (
          <span className={styles.chevronButton}>
            <img
              alt=""
              className={isExpanded ? styles.chevronExpanded : styles.icon}
              src={chevronRight}
            />
          </span>
        ) : (
          <span className={styles.chevronPlaceholder} />
        )}
        <span className={styles.itemButton}>
          <LayerIcon className={styles.icon} size={16} strokeWidth={2} />
          {node.name}
        </span>
        <Eye className={styles.visibilityIcon} size={16} strokeWidth={2} />
      </button>
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
