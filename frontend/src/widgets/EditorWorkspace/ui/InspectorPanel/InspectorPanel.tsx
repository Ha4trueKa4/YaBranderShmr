import type { ComponentType } from 'react'

import type { EditorLayer, EditorLayerByType, EditorLayerType } from '@entities/editorDocument'

import { BackgroundProperties } from './backgroundProperties/BackgroundProperties'
import { ShapeProperties } from './shapeProperties/ShapeProperties'
import { TextProperties } from './textProperties/TextProperties'
import styles from './InspectorPanel.module.css'

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
        <h2>Inspector</h2>
        <p>Select a layer to edit its properties.</p>
      </section>
    )
  }

  const Properties = inspectorByType[selectedLayer.type] as ComponentType<{
    layer: typeof selectedLayer
  }>

  return (
    <section className={styles.panel}>
      <h2>Inspector</h2>
      <Properties layer={selectedLayer} />
    </section>
  )
}
