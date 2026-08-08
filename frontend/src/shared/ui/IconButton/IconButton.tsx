import type { ButtonHTMLAttributes, ReactNode } from 'react'
import styles from './IconButton.module.css'

type IconButtonVariant = 'default' | 'ghost' | 'primary'
type IconButtonSize = 'sm' | 'md' | 'lg'

export interface IconButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  'aria-label': string
  children: ReactNode
  variant?: IconButtonVariant
  size?: IconButtonSize
}

export function IconButton({ children, className, size = 'md', type = 'button', variant = 'default', ...props }: IconButtonProps) {
  return (
    <button {...props} className={[styles.button, styles[variant], styles[size], className].filter(Boolean).join(' ')} type={type}>
      {children}
    </button>
  )
}
