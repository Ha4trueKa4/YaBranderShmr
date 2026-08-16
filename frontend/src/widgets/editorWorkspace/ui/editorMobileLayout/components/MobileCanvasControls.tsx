import { ColorSwatch, IconButton } from '@shared/ui'

import iconHand from '../../editorToolbar/assets/iconHand.svg'
import iconMousePointer from '../../editorToolbar/assets/iconMousePointer.svg'
import iconRotateLeft from '../../editorToolbar/assets/iconRotateLeft.svg'
import iconRotateRight from '../../editorToolbar/assets/iconRotateRight.svg'
import styles from '../EditorMobileLayout.module.css'

interface MobileCanvasControlsProps {
  fillColor: string | null
  onOpenColorPicker: () => void
}

export function MobileCanvasControls({
  fillColor,
  onOpenColorPicker,
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
      <IconButton>
        <img alt="" src={iconHand} />
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
