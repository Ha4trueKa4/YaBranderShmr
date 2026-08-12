import { IconButton } from '@shared/ui'
import iconRotateLeft from '../assets/iconRotateLeft.svg'
import iconRotateRight from '../assets/iconRotateRight.svg'
import iconSearchMinus from '../assets/iconSearchMinus.svg'
import iconSearchPlus from '../assets/iconSearchPlus.svg'
import styles from '../EditorToolbar.module.css'

export type CanvasTool = 'select' | 'pan'

interface CanvasControlsProps {
  activeTool: CanvasTool
  onPanTool: () => void
  onRedo: () => void
  onSelectTool: () => void
  onUndo: () => void
  onZoomIn: () => void
  onZoomOut: () => void
}

export function CanvasControls({
  activeTool,
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
        className={activeTool === 'select' ? styles.activeTool : undefined}
        onClick={onSelectTool}
        size="sm"
      >
        <span className={`${styles.toolIcon} ${styles.selectTool}`} />
      </IconButton>
      <IconButton
        className={activeTool === 'pan' ? styles.activeTool : undefined}
        onClick={onPanTool}
        size="sm"
      >
        <span className={`${styles.toolIcon} ${styles.panTool}`} />
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
