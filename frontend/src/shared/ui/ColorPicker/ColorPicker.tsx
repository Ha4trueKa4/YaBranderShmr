import { useEffect, useState } from 'react'
import { HexColorPicker } from 'react-colorful'

import { Input } from '../Input'

import styles from './ColorPicker.module.css'

const HEX_COLOR_PATTERN = /^#(?:[\dA-Fa-f]{3}|[\dA-Fa-f]{6})$/

export interface ColorPickerProps {
  color: string
  onChange: (color: string) => void
  onChangeEnd?: (color: string) => void
}

export function ColorPicker({
  color,
  onChange,
  onChangeEnd,
}: ColorPickerProps) {
  const [hexColor, setHexColor] = useState(color)

  useEffect(() => {
    setHexColor(color)
  }, [color])

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

  return (
    <div className={styles.picker}>
      <HexColorPicker
        color={color}
        onChange={handleHexColorChange}
        onChangeEnd={onChangeEnd}
      />
      <div className={styles.hexField}>
        <span>HEX</span>
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
  )
}
