import type { ReactNode } from 'react'
import styles from './FormField.module.css'

export interface FormFieldProps {
  children: ReactNode
  error?: string
  label: string
  required?: boolean
  reserveErrorSpace?: boolean
}

export function FormField({
  children,
  error,
  label,
  required = false,
  reserveErrorSpace = false,
}: FormFieldProps) {
  return (
    <div
      className={[styles.field, reserveErrorSpace && styles.reserveErrorSpace]
        .filter(Boolean)
        .join(' ')}
    >
      <span className={styles.label}>
        {label}
        {required && <span className={styles.required}> *</span>}
      </span>
      {children}
      {error && <span className={styles.error}>{error}</span>}
    </div>
  )
}
