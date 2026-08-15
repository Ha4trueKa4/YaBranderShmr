import type { EditorDocument } from '@entities/editorDocument'

import { LayerRenderer } from '../layerRenderer/LayerRenderer'
import { SelectionOverlay } from '../selectionOverlay/SelectionOverlay'
import { TransformHandles } from '../transformHandles/TransformHandles'
import styles from './SceneFrame.module.css'

interface SceneFrameProps {
  document: EditorDocument
  onSelectNode: (nodeId: string) => void
  renderVersion: number
}

export function SceneFrame({
  document,
  onSelectNode,
  renderVersion,
}: SceneFrameProps) {
  return (
    <div className={styles.frame}>
      <svg
        className={styles.canvas}
        viewBox={`0 0 ${document.width} ${document.height}`}
        xmlns="http://www.w3.org/2000/svg"
      >
        <title>Макет проекта</title>
        <LayerRenderer
          document={document}
          onSelectNode={onSelectNode}
          renderVersion={renderVersion}
        />
      </svg>
      <SelectionOverlay />
      <TransformHandles />
    </div>
  )
}
