import type { CSSProperties, InputHTMLAttributes } from 'react'

import styles from './Range.module.css'

export interface RangeProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'value'> {
  value: number
}

export function Range({
  max = 100,
  min = 0,
  style,
  value,
  ...props
}: RangeProps) {
  const rangeValue = Math.min(Math.max(value, Number(min)), Number(max))
  const progress =
    ((rangeValue - Number(min)) / (Number(max) - Number(min))) * 100
  const rangeStyle = {
    ...style,
    '--range-progress': `${progress}%`,
  } as CSSProperties

  return (
    <input
      {...props}
      className={styles.range}
      max={max}
      min={min}
      style={rangeStyle}
      type="range"
      value={rangeValue}
    />
  )
}
