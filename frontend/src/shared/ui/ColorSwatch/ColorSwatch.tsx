import type { ButtonHTMLAttributes } from 'react'

import styles from './ColorSwatch.module.css'

export interface ColorSwatchProps
  extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  color: string
}

export function ColorSwatch({
  color,
  style,
  type = 'button',
  ...props
}: ColorSwatchProps) {
  return (
    <button
      {...props}
      className={styles.swatch}
      style={{ ...style, background: color }}
      type={type}
    />
  )
}
