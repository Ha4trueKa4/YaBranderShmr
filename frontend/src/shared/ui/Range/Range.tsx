import type { CSSProperties, InputHTMLAttributes } from 'react'

import styles from './Range.module.css'

export interface RangeProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'value'> {
  centered?: boolean
  value: number
}

export function Range({
  centered = false,
  max = 100,
  min = 0,
  style,
  value,
  ...props
}: RangeProps) {
  const rangeValue = Math.min(Math.max(value, Number(min)), Number(max))
  const progress =
    ((rangeValue - Number(min)) / (Number(max) - Number(min))) * 100
  const centerProgress = ((0 - Number(min)) / (Number(max) - Number(min))) * 100
  const trackBackground = centered
    ? progress <= centerProgress
      ? `linear-gradient(to right, var(--color-border) 0 ${progress}%, var(--color-accent-primary) ${progress}% ${centerProgress}%, var(--color-border) ${centerProgress}% 100%)`
      : `linear-gradient(to right, var(--color-border) 0 ${centerProgress}%, var(--color-accent-primary) ${centerProgress}% ${progress}%, var(--color-border) ${progress}% 100%)`
    : `linear-gradient(to right, var(--color-accent-primary) 0 ${progress}%, var(--color-border) ${progress}% 100%)`
  const rangeStyle = {
    ...style,
    '--range-background': trackBackground,
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
