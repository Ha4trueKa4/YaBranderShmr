import type { EditorNode } from '@entities/editorDocument'
import type { LucideIcon } from 'lucide-react'
import { Eye, Feather, Folder, Type } from 'lucide-react'

import styles from './LayersPanel.module.css'

interface LayerTreeItemProps {
  depth: number
  node: EditorNode
  onSelectNode: (nodeId: string) => void
  selectedNodeId: string | null
}

const iconByType: Record<EditorNode['type'], LucideIcon> = {
  group: Folder,
  shape: Feather,
  text: Type,
}

export function LayerTreeItem({
  depth,
  node,
  onSelectNode,
  selectedNodeId,
}: LayerTreeItemProps) {
  const isGroup = node.type === 'group'
  const isSelected = node.id === selectedNodeId
  const LayerIcon = iconByType[node.type]
  const indentation = `calc(var(--space-4) + ${depth} * var(--space-5))`

  return (
    <div className={styles.treeItem}>
      <button
        className={isSelected ? styles.selected : styles.layer}
        onClick={() => onSelectNode(node.id)}
        style={{
          paddingInlineStart: indentation,
        }}
        type="button"
      >
        <span className={styles.chevronPlaceholder} />
        <span className={styles.itemButton}>
          <LayerIcon className={styles.icon} size={16} strokeWidth={2} />
          {node.name}
        </span>
        <Eye className={styles.visibilityIcon} size={16} strokeWidth={2} />
      </button>
      {isGroup && (
        <div className={styles.children}>
          {node.children.map((child: EditorNode) => (
            <LayerTreeItem
              depth={depth + 1}
              key={child.id}
              node={child}
              onSelectNode={onSelectNode}
              selectedNodeId={selectedNodeId}
            />
          ))}
        </div>
      )}
    </div>
  )
}
