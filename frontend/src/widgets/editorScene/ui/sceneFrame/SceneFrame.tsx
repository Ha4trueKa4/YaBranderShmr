import type { EditorDocument } from '@entities/editorDocument'

import { LayerRenderer } from '../layerRenderer/LayerRenderer'
import { SelectionOverlay } from '../selectionOverlay/SelectionOverlay'
import { TransformHandles } from '../transformHandles/TransformHandles'
import styles from './SceneFrame.module.css'

interface SceneFrameProps {
  document: EditorDocument
}

export function SceneFrame({ document }: SceneFrameProps) {
  return (
    <div className={styles.frame}>
      <svg
        className={styles.canvas}
        viewBox={`0 0 ${document.width} ${document.height}`}
        xmlns="http://www.w3.org/2000/svg"
      >
        <title>Макет проекта</title>
        <LayerRenderer document={document} />
      </svg>
      <SelectionOverlay />
      <TransformHandles />
    </div>
  )
}
