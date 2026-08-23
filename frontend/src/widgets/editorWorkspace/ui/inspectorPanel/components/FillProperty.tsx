import { ColorPicker, ColorSwatch } from '@shared/ui'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'

import styles from './FillProperty.module.css'
import { OpacityProperty } from './OpacityProperty'

interface FillPropertyProps {
  fill: string
  onColorChangeEnd?: () => void
  onColorChangeStart?: () => void
  onFillChange: (fill: string) => void
  onEyeDropperEnd?: () => void
  onEyeDropperStart?: () => void
  onOpacityChange?: (opacity: number) => void
  opacity?: number
  opacityLabel?: string
  title?: string
}

export function FillProperty({
  fill,
  onColorChangeEnd,
  onColorChangeStart,
  onFillChange,
  onEyeDropperEnd,
  onEyeDropperStart,
  onOpacityChange,
  opacity = 1,
  opacityLabel,
  title,
}: FillPropertyProps) {
  const { t } = useTranslation()
  const [draftFill, setDraftFill] = useState(fill)
  const [isColorPickerOpen, setIsColorPickerOpen] = useState(false)
  const colorControlRef = useRef<HTMLDivElement>(null)
  const isColorChangeActiveRef = useRef(false)

  const beginColorChange = useCallback(() => {
    if (!isColorChangeActiveRef.current) {
      onColorChangeStart?.()
      isColorChangeActiveRef.current = true
    }
  }, [onColorChangeStart])

  const endColorChange = useCallback(() => {
    if (isColorChangeActiveRef.current) {
      onColorChangeEnd?.()
      isColorChangeActiveRef.current = false
    }
  }, [onColorChangeEnd])

  const closeColorPicker = useCallback(() => {
    endColorChange()
    setIsColorPickerOpen(false)
  }, [endColorChange])

  useEffect(() => {
    if (!isColorPickerOpen) {
      return
    }

    function handlePointerDown(event: PointerEvent) {
      if (
        event.target instanceof Node &&
        !colorControlRef.current?.contains(event.target)
      ) {
        closeColorPicker()
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        closeColorPicker()
      }
    }

    document.addEventListener('pointerdown', handlePointerDown)
    document.addEventListener('keydown', handleKeyDown)

    return () => {
      document.removeEventListener('pointerdown', handlePointerDown)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [isColorPickerOpen, closeColorPicker])

  useEffect(() => {
    setDraftFill(fill)
  }, [fill])

  function handleFillChange(color: string) {
    beginColorChange()
    setDraftFill(color)
    onFillChange(color)
  }

  return (
    <section className={styles.property}>
      <p className={styles.title}>{t(title ?? 'Fill')}</p>
      <div className={styles.colorValue}>
        <div className={styles.colorControl} ref={colorControlRef}>
          <ColorSwatch
            color={draftFill}
            onClick={() => {
              if (isColorPickerOpen) {
                closeColorPicker()
              } else {
                setIsColorPickerOpen(true)
              }
            }}
          />
          {isColorPickerOpen && (
            <div className={styles.colorPickerPopover}>
              <ColorPicker
                color={draftFill}
                onChange={handleFillChange}
                onChangeEnd={endColorChange}
                onEyeDropperEnd={onEyeDropperEnd}
                onEyeDropperStart={onEyeDropperStart}
              />
            </div>
          )}
        </div>
        <div>
          <p className={styles.hex}>{draftFill.toUpperCase()}</p>
        </div>
      </div>
      {onOpacityChange && (
        <OpacityProperty
          label={opacityLabel ?? 'Fill opacity'}
          onOpacityChange={onOpacityChange}
          opacity={opacity}
        />
      )}
    </section>
  )
}
