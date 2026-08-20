import { IconButton, Tooltip } from '@shared/ui'
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
  const zoomPercent = Math.round(zoom * 100)

  return (
    <div className={styles.canvasControls}>
      <IconButton
        className={activeTool === 'select' ? styles.activeTool : undefined}
        onClick={() => onToolChange('select')}
        size="sm"
        title="Select tool"
      >
        <span className={`${styles.toolIcon} ${styles.selectTool}`} />
      </IconButton>
      <IconButton
        className={activeTool === 'hand' ? styles.activeTool : undefined}
        onClick={() => onToolChange('hand')}
        size="sm"
        title="Pan canvas"
      >
        <span className={`${styles.toolIcon} ${styles.panTool}`} />
      </IconButton>
      <span className={styles.divider} />
      <Tooltip content={`Увеличить масштаб (${zoomPercent}%)`}>
        <IconButton
          disabled={zoom >= 2}
          onClick={onZoomIn}
          size="sm"
          title="Zoom in"
        >
          <ZoomIn color="#71717A" size={18} strokeWidth={2.5} />
        </IconButton>
      </Tooltip>
      <Tooltip content={`Уменьшить масштаб (${zoomPercent}%)`}>
        <IconButton
          disabled={zoom <= 0.5}
          onClick={onZoomOut}
          size="sm"
          title="Zoom out"
        >
          <ZoomOut color="#71717A" size={18} strokeWidth={2.5} />
        </IconButton>
      </Tooltip>
      <span className={styles.divider} />
      <IconButton onClick={onUndo} size="sm" title="Undo">
        <img alt="" src={iconRotateLeft} />
      </IconButton>
      <IconButton onClick={onRedo} size="sm" title="Redo">
        <img alt="" src={iconRotateRight} />
      </IconButton>
    </div>
  )
}
