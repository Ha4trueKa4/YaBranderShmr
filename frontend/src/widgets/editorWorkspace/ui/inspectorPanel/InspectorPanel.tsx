import type { EditorNode } from '@entities/editorDocument'

import { getLayerPresentation } from '../layerPresentation'
import { GroupProperties } from './groupProperties/GroupProperties'
import styles from './InspectorPanel.module.css'
import { ShapeProperties } from './shapeProperties/ShapeProperties'
import { TextProperties } from './textProperties/TextProperties'

interface InspectorPanelProps {
  onShapeFillChange: (nodeId: string, color: string) => void
  onShapeFillOpacityChange: (nodeId: string, opacity: number) => void
  selectedNode: EditorNode | null
}

export function InspectorPanel({
  onShapeFillChange,
  onShapeFillOpacityChange,
  selectedNode,
}: InspectorPanelProps) {
  if (!selectedNode) {
    return (
      <section className={styles.panel}>
        <div className={styles.header}>
          <h2>Inspector</h2>
        </div>
        <div className={styles.content}>
          <p>Select a layer to edit its properties.</p>
        </div>
      </section>
    )
  }

  if (selectedNode.type === 'group') {
    return (
      <section className={styles.panel}>
        <div className={styles.header}>
          <h2>Inspector</h2>
          <span className={styles.typeLabel}>
            {getLayerPresentation(selectedNode).label}
          </span>
        </div>
        <div className={styles.content}>
          <GroupProperties layer={selectedNode} />
        </div>
      </section>
    )
  }

  if (selectedNode.type === 'raw') {
    return (
      <section className={styles.panel}>
        <div className={styles.header}>
          <h2>Inspector</h2>
          <span className={styles.typeLabel}>
            {getLayerPresentation(selectedNode).label}
          </span>
        </div>
        <div className={styles.content}>
          <p>
            This SVG layer is locked because its features are not editable yet.
          </p>
        </div>
      </section>
    )
  }

  if (selectedNode.type === 'shape') {
    return (
      <section className={styles.panel}>
        <div className={styles.header}>
          <h2>Inspector</h2>
          <span className={styles.typeLabel}>
            {getLayerPresentation(selectedNode).label}
          </span>
        </div>
        <div className={styles.content}>
          <ShapeProperties
            layer={selectedNode}
            onFillChange={(color) => onShapeFillChange(selectedNode.id, color)}
            onFillOpacityChange={(opacity) =>
              onShapeFillOpacityChange(selectedNode.id, opacity)
            }
          />
        </div>
      </section>
    )
  }

  return (
    <section className={styles.panel}>
      <div className={styles.header}>
        <h2>Inspector</h2>
        <span className={styles.typeLabel}>
          {getLayerPresentation(selectedNode).label}
        </span>
      </div>
      <div className={styles.content}>
        <TextProperties layer={selectedNode} />
      </div>
    </section>
  )
}
