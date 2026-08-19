import { ColorSwatch, IconButton } from '@shared/ui'
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
  return (
    <div className={styles.canvasControls}>
      {fillColor ? (
        <ColorSwatch
          className={styles.fillSwatch}
          color={fillColor}
          onClick={onOpenColorPicker}
        />
      ) : (
        <IconButton className={styles.activeTool}>
          <img alt="" src={iconMousePointer} />
        </IconButton>
      )}
      <IconButton
        className={activeTool === 'hand' ? styles.activeTool : undefined}
        onClick={() => onToolChange(activeTool === 'hand' ? 'select' : 'hand')}
      >
        <span className={styles.handIcon} />
      </IconButton>
      <span />
      <IconButton disabled={zoom >= 2} onClick={onZoomIn}>
        <ZoomIn size={18} />
      </IconButton>
      <IconButton disabled={zoom <= 0.5} onClick={onZoomOut}>
        <ZoomOut size={18} />
      </IconButton>
      <span />
      <IconButton onClick={onZoomReset}>
        <span className={styles.fitIcon}>⌗</span>
      </IconButton>
    </div>
  )
}
