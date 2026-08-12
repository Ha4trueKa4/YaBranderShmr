import { LayerRenderer } from '../layerRenderer/LayerRenderer'
import { SelectionOverlay } from '../selectionOverlay/SelectionOverlay'
import { TransformHandles } from '../transformHandles/TransformHandles'
import styles from './SceneFrame.module.css'

export function SceneFrame() {
  return (
    <div className={styles.frame}>
      {/* LayerRenderer рисует слои; overlay и handles включаются только для selectedLayer. */}
      <LayerRenderer />
      <SelectionOverlay />
      <TransformHandles />
      <span>Scene frame</span>
    </div>
  )
}
