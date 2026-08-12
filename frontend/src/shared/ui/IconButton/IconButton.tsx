import type { ButtonHTMLAttributes, ReactNode } from 'react'
import styles from './IconButton.module.css'

export interface IconButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement> {
  children: ReactNode
  size?: 'sm' | 'md'
}

export function IconButton({
  children,
  className,
  size = 'md',
  type = 'button',
  ...props
}: IconButtonProps) {
  return (
    <button
      {...props}
      className={[styles.button, styles[size], className]
        .filter(Boolean)
        .join(' ')}
      type={type}
    >
      {children}
    </button>
  )
}
