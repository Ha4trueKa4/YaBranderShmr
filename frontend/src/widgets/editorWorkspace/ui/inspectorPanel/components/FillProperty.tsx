import { ColorPicker, ColorSwatch, Range } from '@shared/ui'
import { useEffect, useRef, useState } from 'react'

import styles from './FillProperty.module.css'

interface FillPropertyProps {
  fill: string
  opacity?: number
}

export function FillProperty({ fill, opacity = 1 }: FillPropertyProps) {
  const [draftFill, setDraftFill] = useState(fill)
  const [isColorPickerOpen, setIsColorPickerOpen] = useState(false)
  const colorControlRef = useRef<HTMLDivElement>(null)
  const opacityPercent = Math.round(opacity * 100)

  useEffect(() => {
    if (!isColorPickerOpen) {
      return
    }

    function handlePointerDown(event: PointerEvent) {
      if (
        event.target instanceof Node &&
        !colorControlRef.current?.contains(event.target)
      ) {
        setIsColorPickerOpen(false)
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setIsColorPickerOpen(false)
      }
    }

    document.addEventListener('pointerdown', handlePointerDown)
    document.addEventListener('keydown', handleKeyDown)

    return () => {
      document.removeEventListener('pointerdown', handlePointerDown)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [isColorPickerOpen])

  return (
    <section className={styles.property}>
      <p className={styles.title}>Fill</p>
      <div className={styles.colorValue}>
        <div className={styles.colorControl} ref={colorControlRef}>
          <ColorSwatch
            color={draftFill}
            onClick={() => setIsColorPickerOpen((isOpen) => !isOpen)}
          />
          {isColorPickerOpen && (
            <div className={styles.colorPickerPopover}>
              <ColorPicker color={draftFill} onChange={setDraftFill} />
            </div>
          )}
        </div>
        <div>
          <p className={styles.hex}>{draftFill.toUpperCase()}</p>
          <p className={styles.colorName}>Violet Primary</p>
        </div>
      </div>
      <div className={styles.opacityHeader}>
        <span>Opacity</span>
        <strong>{opacityPercent}%</strong>
      </div>
      {/* TODO: сохранять draftFill и opacity в EditorStore через feature редактирования свойств. */}
      <Range disabled max="100" min="0" value={opacityPercent} />
    </section>
  )
}
