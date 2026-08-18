import { IconButton } from '@shared/ui'
import type { CanvasTool } from '@widgets/editorScene'
import iconRotateLeft from '../assets/iconRotateLeft.svg'
import iconRotateRight from '../assets/iconRotateRight.svg'
import iconSearchMinus from '../assets/iconSearchMinus.svg'
import iconSearchPlus from '../assets/iconSearchPlus.svg'
import styles from '../EditorToolbar.module.css'

interface CanvasControlsProps {
  activeTool: CanvasTool
  onToolChange: (tool: CanvasTool) => void
  onRedo: () => void
  onUndo: () => void
  onZoomIn: () => void
  onZoomOut: () => void
}

export function CanvasControls({
  activeTool,
  onToolChange,
  onRedo,
  onUndo,
  onZoomIn,
  onZoomOut,
}: CanvasControlsProps) {
  return (
    <div className={styles.canvasControls}>
      <IconButton
        className={activeTool === 'select' ? styles.activeTool : undefined}
        onClick={() => onToolChange('select')}
        size="sm"
      >
        <span className={`${styles.toolIcon} ${styles.selectTool}`} />
      </IconButton>
      <IconButton
        className={activeTool === 'hand' ? styles.activeTool : undefined}
        onClick={() => onToolChange('hand')}
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
