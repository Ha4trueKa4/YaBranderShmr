import type { EditorLayer } from '@entities/editorDocument'
import { EditorScene } from '@widgets/editorScene'
import styles from './EditorWorkspace.module.css'
import { EditorStatusBar } from './editorStatusBar/EditorStatusBar'
import { EditorToolbar } from './editorToolbar/EditorToolbar'
import { InspectorPanel } from './inspectorPanel/InspectorPanel'
import { LayersPanel } from './layersPanel/LayersPanel'

/** Временные данные для вёрстки; заменить на выбранный слой из editorStore. */
const previewSelectedLayer: EditorLayer = {
  fill: '#7C3AED',
  id: 'preview-icon-shape',
  name: 'Icon shape',
  stroke: '#06B6D4',
  type: 'shape',
  x: 120,
  y: 140,
}

export function EditorWorkspace() {
  return (
    <section className={styles.workspace}>
      {/* Подключить editorStore: document, selectedLayerId, zoom и историю изменений. */}
      <div className={styles.toolbar}>
        <EditorToolbar />
      </div>
      <aside className={styles.layers}>
        <LayersPanel />
      </aside>
      <div className={styles.scene}>
        <EditorScene />
      </div>
      <aside className={styles.inspector}>
        <InspectorPanel selectedLayer={previewSelectedLayer} />
      </aside>
      <div className={styles.status}>
        <EditorStatusBar />
      </div>
    </section>
  )
}
