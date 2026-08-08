import type { InputHTMLAttributes, ReactNode } from 'react'
import styles from './Input.module.css'

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  invalid?: boolean
  leadingContent?: ReactNode
  trailingContent?: ReactNode
}

export function Input({ className, invalid = false, leadingContent, trailingContent, ...props }: InputProps) {
  return (
    <span className={[styles.root, invalid && styles.invalid, className].filter(Boolean).join(' ')}>
      {leadingContent && <span className={styles.adornment}>{leadingContent}</span>}
      <input {...props} aria-invalid={invalid || props['aria-invalid']} className={styles.input} />
      {trailingContent && <span className={styles.adornment}>{trailingContent}</span>}
    </span>
  )
}
