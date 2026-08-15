import { HexColorPicker } from 'react-colorful'

import styles from './ColorPicker.module.css'

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
  return (
    <HexColorPicker
      className={styles.picker}
      color={color}
      onChange={onChange}
      onChangeEnd={onChangeEnd}
    />
  )
}
