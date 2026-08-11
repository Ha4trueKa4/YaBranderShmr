import { IconButton } from '@shared/ui'
import iconHand from '../assets/iconHand.svg'
import iconMousePointer from '../assets/iconMousePointer.svg'
import iconRotateLeft from '../assets/iconRotateLeft.svg'
import iconRotateRight from '../assets/iconRotateRight.svg'
import iconSearchMinus from '../assets/iconSearchMinus.svg'
import iconSearchPlus from '../assets/iconSearchPlus.svg'
import styles from '../EditorToolbar.module.css'

interface CanvasControlsProps {
  onPanTool: () => void
  onRedo: () => void
  onSelectTool: () => void
  onUndo: () => void
  onZoomIn: () => void
  onZoomOut: () => void
}

export function CanvasControls({
  onPanTool,
  onRedo,
  onSelectTool,
  onUndo,
  onZoomIn,
  onZoomOut,
}: CanvasControlsProps) {
  return (
    <div className={styles.canvasControls}>
      <IconButton
        className={styles.activeTool}
        onClick={onSelectTool}
        size="sm"
      >
        <img alt="" src={iconMousePointer} />
      </IconButton>
      <IconButton onClick={onPanTool} size="sm">
        <img alt="" src={iconHand} />
      </IconButton>
      <span className={styles.divider} />
      <IconButton onClick={onZoomIn} size="sm">
        <img alt="" src={iconSearchPlus} />
      </IconButton>
      <IconButton onClick={onZoomOut} size="sm">
        <img alt="" src={iconSearchMinus} />
      </IconButton>
      <span className={styles.divider} />
      <IconButton onClick={onUndo} size="sm">
        <img alt="" src={iconRotateLeft} />
      </IconButton>
      <IconButton onClick={onRedo} size="sm">
        <img alt="" src={iconRotateRight} />
      </IconButton>
    </div>
  )
}
