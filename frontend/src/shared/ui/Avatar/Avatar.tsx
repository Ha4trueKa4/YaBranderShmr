import type { HTMLAttributes } from 'react'

import styles from './Avatar.module.css'

export interface AvatarProps extends HTMLAttributes<HTMLSpanElement> {
  initials: string
  size?: 'sm' | 'md' | 'lg'
}

export function Avatar({
  className,
  initials,
  size = 'md',
  ...props
}: AvatarProps) {
  return (
    <span
      {...props}
      className={[styles.avatar, styles[size], className]
        .filter(Boolean)
        .join(' ')}
    >
      {initials}
    </span>
  )
}
