import type {
  EditorElement,
  EditorElementByType,
  EditorElementType,
} from '@entities/editorDocument'
import type { ComponentType } from 'react'

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
  selectedLayer: EditorElement | null
}

export function InspectorPanel({ selectedLayer }: InspectorPanelProps) {
  if (!selectedLayer) {
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

  const Properties = inspectorByType[selectedLayer.type] as ComponentType<{
    layer: typeof selectedLayer
  }>

  return (
    <section className={styles.panel}>
      <div className={styles.header}>
        <h2>Inspector</h2>
      </div>
      <div className={styles.content}>
        {/* Поля свойств должны вызывать feature обновления слоя, а не менять layer напрямую. */}
        <Properties layer={selectedLayer} />
      </div>
    </section>
  )
}
