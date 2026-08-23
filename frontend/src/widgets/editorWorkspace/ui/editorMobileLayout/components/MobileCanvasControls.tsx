import { IconButton, Tooltip } from '@shared/ui'
import type { CanvasTool } from '@widgets/editorScene'
import { ZoomIn, ZoomOut } from 'lucide-react'
import { useTranslation } from 'react-i18next'

import iconRotateLeft from '../../editorToolbar/assets/iconRotateLeft.svg'
import iconRotateRight from '../../editorToolbar/assets/iconRotateRight.svg'
import styles from '../EditorMobileLayout.module.css'

interface MobileCanvasControlsProps {
  activeTool: CanvasTool
  canRedo: boolean
  canUndo: boolean
  onToolChange: (tool: CanvasTool) => void
  onRedo: () => void
  onUndo: () => void
  onZoomIn: () => void
  onZoomOut: () => void
  zoom: number
}

export function MobileCanvasControls({
  activeTool,
  canRedo,
  canUndo,
  onToolChange,
  onRedo,
  onUndo,
  onZoomIn,
  onZoomOut,
  zoom,
}: MobileCanvasControlsProps) {
  const { t } = useTranslation()
  const zoomPercent = Math.round(zoom * 100)

  return (
    <div className={styles.canvasControls}>
      <IconButton
        className={activeTool === 'select' ? styles.activeTool : undefined}
        onClick={() => onToolChange('select')}
        title={t('Select tool')}
      >
        <span className={styles.selectIcon} />
      </IconButton>
      <IconButton
        className={activeTool === 'hand' ? styles.activeTool : undefined}
        onClick={() => onToolChange(activeTool === 'hand' ? 'select' : 'hand')}
        title={t('Pan canvas')}
      >
        <span className={styles.handIcon} />
      </IconButton>
      <span />
      <Tooltip content={t('Zoom in ({{percent}}%)', { percent: zoomPercent })}>
        <IconButton
          disabled={zoom >= 2}
          onClick={onZoomIn}
          title={t('Zoom in')}
        >
          <ZoomIn color="#71717A" size={18} strokeWidth={2.5} />
        </IconButton>
      </Tooltip>
      <Tooltip content={t('Zoom out ({{percent}}%)', { percent: zoomPercent })}>
        <IconButton
          disabled={zoom <= 0.5}
          onClick={onZoomOut}
          title={t('Zoom out')}
        >
          <ZoomOut color="#71717A" size={18} strokeWidth={2.5} />
        </IconButton>
      </Tooltip>
      <span />
      <IconButton disabled={!canUndo} onClick={onUndo} title={t('Undo')}>
        <img alt="" src={iconRotateLeft} />
      </IconButton>
      <IconButton disabled={!canRedo} onClick={onRedo} title={t('Redo')}>
        <img alt="" src={iconRotateRight} />
      </IconButton>
    </div>
  )
}
