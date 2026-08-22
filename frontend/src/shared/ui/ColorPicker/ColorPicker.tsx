import { Pipette } from 'lucide-react'
import { useEffect, useState } from 'react'
import { HexColorPicker } from 'react-colorful'
import { useTranslation } from 'react-i18next'

import { IconButton } from '../IconButton'
import { Input } from '../Input'

import styles from './ColorPicker.module.css'

const HEX_COLOR_PATTERN = /^#(?:[\dA-Fa-f]{3}|[\dA-Fa-f]{6})$/

interface EyeDropper {
  open: () => Promise<{ sRGBHex: string }>
}

type EyeDropperConstructor = new () => EyeDropper

function getEyeDropperConstructor() {
  return (window as Window & { EyeDropper?: EyeDropperConstructor }).EyeDropper
}

export interface ColorPickerProps {
  color: string
  onChange: (color: string) => void
  onChangeEnd?: (color: string) => void
  onEyeDropperEnd?: () => void
  onEyeDropperStart?: () => void
}

export function ColorPicker({
  color,
  onChange,
  onChangeEnd,
  onEyeDropperEnd,
  onEyeDropperStart,
}: ColorPickerProps) {
  const { t } = useTranslation()
  const [hexColor, setHexColor] = useState(color)
  const [isEyeDropperSupported, setIsEyeDropperSupported] = useState(false)

  useEffect(() => {
    setHexColor(color)
  }, [color])

  useEffect(() => {
    setIsEyeDropperSupported(Boolean(getEyeDropperConstructor()))
  }, [])

  function handleHexColorChange(nextColor: string) {
    setHexColor(nextColor)

    if (HEX_COLOR_PATTERN.test(nextColor)) {
      onChange(nextColor)
    }
  }

  function handleHexColorBlur() {
    if (!HEX_COLOR_PATTERN.test(hexColor)) {
      setHexColor(color)
      return
    }

    onChangeEnd?.(hexColor)
  }

  async function handleEyeDropperClick() {
    const EyeDropper = getEyeDropperConstructor()

    if (!EyeDropper) {
      return
    }

    onEyeDropperStart?.()

    try {
      const { sRGBHex } = await new EyeDropper().open()
      handleHexColorChange(sRGBHex)
      onChangeEnd?.(sRGBHex)
    } catch {
      // Closing the browser color picker does not change the selected color.
    } finally {
      onEyeDropperEnd?.()
    }
  }

  return (
    <div className={styles.picker}>
      <HexColorPicker
        color={color}
        onChange={handleHexColorChange}
        onChangeEnd={onChangeEnd}
      />
      <div className={styles.hexField}>
        <span>{t('HEX')}</span>
        <div className={styles.hexControls}>
          {isEyeDropperSupported && (
            <IconButton
              onClick={handleEyeDropperClick}
              size="sm"
              title={t('Pick color')}
            >
              <Pipette size={16} />
            </IconButton>
          )}
          <Input
            autoComplete="off"
            invalid={hexColor.length > 0 && !HEX_COLOR_PATTERN.test(hexColor)}
            maxLength={7}
            onBlur={handleHexColorBlur}
            onChange={(event) => handleHexColorChange(event.target.value)}
            placeholder="#RRGGBB"
            spellCheck={false}
            value={hexColor}
          />
        </div>
      </div>
    </div>
  )
}
