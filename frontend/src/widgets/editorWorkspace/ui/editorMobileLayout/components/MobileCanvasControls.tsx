import { ColorSwatch, IconButton } from '@shared/ui'
import type { CanvasTool } from '@widgets/editorScene'

import iconMousePointer from '../../editorToolbar/assets/iconMousePointer.svg'
import iconRotateLeft from '../../editorToolbar/assets/iconRotateLeft.svg'
import iconRotateRight from '../../editorToolbar/assets/iconRotateRight.svg'
import styles from '../EditorMobileLayout.module.css'

interface MobileCanvasControlsProps {
  activeTool: CanvasTool
  fillColor: string | null
  onOpenColorPicker: () => void
  onToolChange: (tool: CanvasTool) => void
}

export function MobileCanvasControls({
  activeTool,
  fillColor,
  onOpenColorPicker,
  onToolChange,
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
      <IconButton>
        <img alt="" src={iconRotateLeft} />
      </IconButton>
      <IconButton>
        <img alt="" src={iconRotateRight} />
      </IconButton>
      <span />
      <IconButton>
        <span className={styles.fitIcon}>⌗</span>
      </IconButton>
    </div>
  )
}
