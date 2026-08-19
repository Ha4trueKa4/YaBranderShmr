import { ColorSwatch, IconButton, Tooltip } from '@shared/ui'
import type { CanvasTool } from '@widgets/editorScene'
import { ZoomIn, ZoomOut } from 'lucide-react'

import iconMousePointer from '../../editorToolbar/assets/iconMousePointer.svg'
import styles from '../EditorMobileLayout.module.css'

interface MobileCanvasControlsProps {
  activeTool: CanvasTool
  fillColor: string | null
  onOpenColorPicker: () => void
  onToolChange: (tool: CanvasTool) => void
  onZoomIn: () => void
  onZoomOut: () => void
  onZoomReset: () => void
  zoom: number
}

export function MobileCanvasControls({
  activeTool,
  fillColor,
  onOpenColorPicker,
  onToolChange,
  onZoomIn,
  onZoomOut,
  onZoomReset,
  zoom,
}: MobileCanvasControlsProps) {
  const zoomPercent = Math.round(zoom * 100)

  return (
    <div className={styles.canvasControls}>
      {fillColor ? (
        <ColorSwatch
          className={styles.fillSwatch}
          color={fillColor}
          onClick={onOpenColorPicker}
        />
      ) : (
        <IconButton className={styles.activeTool} title="Select tool">
          <img alt="" src={iconMousePointer} />
        </IconButton>
      )}
      <IconButton
        className={activeTool === 'hand' ? styles.activeTool : undefined}
        onClick={() => onToolChange(activeTool === 'hand' ? 'select' : 'hand')}
        title="Pan canvas"
      >
        <span className={styles.handIcon} />
      </IconButton>
      <span />
      <Tooltip content={`Увеличить масштаб (${zoomPercent}%)`}>
        <IconButton disabled={zoom >= 2} onClick={onZoomIn} title="Zoom in">
          <ZoomIn color="#71717A" size={18} strokeWidth={2.5} />
        </IconButton>
      </Tooltip>
      <Tooltip content={`Уменьшить масштаб (${zoomPercent}%)`}>
        <IconButton disabled={zoom <= 0.5} onClick={onZoomOut} title="Zoom out">
          <ZoomOut color="#71717A" size={18} strokeWidth={2.5} />
        </IconButton>
      </Tooltip>
      <span />
      <IconButton onClick={onZoomReset} title="Fit canvas">
        <span className={styles.fitIcon}>⌗</span>
      </IconButton>
    </div>
  )
}
