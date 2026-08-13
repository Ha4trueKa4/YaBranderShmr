import type {
  EditorLayer,
  EditorLayerByType,
  EditorLayerType,
} from '@entities/editorDocument'
import type { ComponentType } from 'react'

import { BackgroundProperties } from './backgroundProperties/BackgroundProperties'
import styles from './InspectorPanel.module.css'
import { ShapeProperties } from './shapeProperties/ShapeProperties'
import { TextProperties } from './textProperties/TextProperties'

type InspectorByType = {
  [TType in EditorLayerType]: ComponentType<{
    layer: EditorLayerByType<TType>
  }>
}

const inspectorByType = {
  background: BackgroundProperties,
  shape: ShapeProperties,
  text: TextProperties,
} satisfies InspectorByType

interface InspectorPanelProps {
  selectedLayer: EditorLayer | null
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
