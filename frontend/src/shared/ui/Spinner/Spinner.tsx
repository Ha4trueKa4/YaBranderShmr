import type { HTMLAttributes } from 'react'

import styles from './Spinner.module.css'

export interface SpinnerProps extends HTMLAttributes<HTMLSpanElement> {
  size?: 'sm' | 'md' | 'lg'
}

export function Spinner({ className, size = 'md', ...props }: SpinnerProps) {
  return (
    <span
      {...props}
      className={[styles.spinner, styles[size], className]
        .filter(Boolean)
        .join(' ')}
    />
  )
}
