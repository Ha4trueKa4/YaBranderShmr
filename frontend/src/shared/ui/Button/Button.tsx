import type { ButtonHTMLAttributes, CSSProperties, ReactNode } from 'react'

import { Spinner } from '@/shared'
import styles from './Button.module.css'

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  children: ReactNode
  fullWidth?: boolean
  loading?: boolean
  minWidth?: CSSProperties['minWidth']
  size?: 'sm' | 'md' | 'lg'
  variant?: 'primary' | 'secondary' | 'ghost' | 'outline' | 'danger'
}

export function Button({
  children,
  className,
  disabled,
  fullWidth = false,
  loading = false,
  minWidth,
  size = 'md',
  style,
  type = 'button',
  variant = 'primary',
  ...props
}: ButtonProps) {
  return (
    <button
      {...props}
      className={[
        styles.button,
        styles[variant],
        styles[size],
        fullWidth && styles.fullWidth,
        loading && styles.loading,
        className,
      ]
        .filter(Boolean)
        .join(' ')}
      disabled={disabled || loading}
      style={{ ...style, minWidth }}
      type={type}
    >
      {loading ? (
        <>
          <span className={styles.loadingLabel}>{children}</span>
          <span className={styles.loadingSpinner}>
            <Spinner size="sm" />
          </span>
        </>
      ) : (
        children
      )}
    </button>
  )
}
