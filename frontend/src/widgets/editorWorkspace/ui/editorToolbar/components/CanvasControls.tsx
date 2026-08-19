import { IconButton } from '@shared/ui'
import type { CanvasTool } from '@widgets/editorScene'
import { ZoomIn, ZoomOut } from 'lucide-react'
import iconRotateLeft from '../assets/iconRotateLeft.svg'
import iconRotateRight from '../assets/iconRotateRight.svg'
import styles from '../EditorToolbar.module.css'

interface CanvasControlsProps {
  activeTool: CanvasTool
  onToolChange: (tool: CanvasTool) => void
  onRedo: () => void
  onUndo: () => void
  onZoomIn: () => void
  onZoomOut: () => void
  zoom: number
}

export function CanvasControls({
  activeTool,
  onToolChange,
  onRedo,
  onUndo,
  onZoomIn,
  onZoomOut,
  zoom,
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
      <IconButton disabled={zoom >= 2} onClick={onZoomIn} size="sm">
        <ZoomIn size={18} />
      </IconButton>
      <IconButton disabled={zoom <= 0.5} onClick={onZoomOut} size="sm">
        <ZoomOut size={18} />
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
