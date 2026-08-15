import type {
  EditorElementByType,
  EditorElementType,
  EditorNode,
} from '@entities/editorDocument'
import type { ComponentType } from 'react'

import { GroupProperties } from './groupProperties/GroupProperties'
import styles from './InspectorPanel.module.css'
import { ShapeProperties } from './shapeProperties/ShapeProperties'
import { TextProperties } from './textProperties/TextProperties'

type InspectorByType = {
  [TType in EditorElementType]: ComponentType<{
    layer: EditorElementByType<TType>
  }>
}

const inspectorByType = {
  shape: ShapeProperties,
  text: TextProperties,
} satisfies InspectorByType

interface InspectorPanelProps {
  selectedNode: EditorNode | null
}

export function InspectorPanel({ selectedNode }: InspectorPanelProps) {
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
        </div>
        <div className={styles.content}>
          <GroupProperties layer={selectedNode} />
        </div>
      </section>
    )
  }

  const Properties = inspectorByType[selectedNode.type] as ComponentType<{
    layer: typeof selectedNode
  }>

  return (
    <section className={styles.panel}>
      <div className={styles.header}>
        <h2>Inspector</h2>
      </div>
      <div className={styles.content}>
        {/* Поля свойств должны вызывать feature обновления слоя, а не менять layer напрямую. */}
        <Properties layer={selectedNode} />
      </div>
    </section>
  )
}
