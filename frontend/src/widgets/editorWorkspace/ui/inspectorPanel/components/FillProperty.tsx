import { ColorPicker, ColorSwatch, Range } from '@shared/ui'
import { useEffect, useRef, useState } from 'react'

import styles from './FillProperty.module.css'

interface FillPropertyProps {
  fill: string
  onFillChange: (fill: string) => void
  onOpacityChange?: (opacity: number) => void
  opacity?: number
  title?: string
}

export function FillProperty({
  fill,
  onFillChange,
  onOpacityChange,
  opacity = 1,
  title = 'Fill',
}: FillPropertyProps) {
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

  useEffect(() => {
    setDraftFill(fill)
  }, [fill])

  function handleFillChange(color: string) {
    setDraftFill(color)
    onFillChange(color)
  }

  function handleOpacityChange(opacityPercent: number) {
    onOpacityChange?.(opacityPercent / 100)
  }

  return (
    <section className={styles.property}>
      <p className={styles.title}>{title}</p>
      <div className={styles.colorValue}>
        <div className={styles.colorControl} ref={colorControlRef}>
          <ColorSwatch
            color={draftFill}
            onClick={() => setIsColorPickerOpen((isOpen) => !isOpen)}
          />
          {isColorPickerOpen && (
            <div className={styles.colorPickerPopover}>
              <ColorPicker color={draftFill} onChange={handleFillChange} />
            </div>
          )}
        </div>
        <div>
          <p className={styles.hex}>{draftFill.toUpperCase()}</p>
          <p className={styles.colorName}>Violet Primary</p>
        </div>
      </div>
      {onOpacityChange && (
        <>
          <div className={styles.opacityHeader}>
            <span>Opacity</span>
            <strong>{opacityPercent}%</strong>
          </div>
          <Range
            max="100"
            min="0"
            onChange={(event) =>
              handleOpacityChange(Number(event.target.value))
            }
            value={opacityPercent}
          />
        </>
      )}
    </section>
  )
}
