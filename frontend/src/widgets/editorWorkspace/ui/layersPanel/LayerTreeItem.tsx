import type { EditorNode } from '@entities/editorDocument'
import { Eye } from 'lucide-react'

import { getLayerPresentation } from '../layerPresentation'
import styles from './LayersPanel.module.css'

interface LayerTreeItemProps {
  depth: number
  node: EditorNode
  onSelectNode: (nodeId: string) => void
  selectedNodeId: string | null
}

export function LayerTreeItem({
  depth,
  node,
  onSelectNode,
  selectedNodeId,
}: LayerTreeItemProps) {
  const isGroup = node.type === 'group'
  const isSelected = node.id === selectedNodeId
  const presentation = getLayerPresentation(node)
  const LayerIcon = presentation.icon
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
          <span className={styles.layerName}>{node.name}</span>
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
