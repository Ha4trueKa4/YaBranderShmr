import type { ReactNode } from 'react'
import styles from './FormField.module.css'

export interface FormFieldProps {
  children: ReactNode
  error?: string
  htmlFor: string
  label: string
  required?: boolean
  reserveErrorSpace?: boolean
}

export function FormField({
  children,
  error,
  htmlFor,
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
      <label className={styles.label} htmlFor={htmlFor}>
        {label}
        {required && <span className={styles.required}> *</span>}
      </label>
      {children}
      {error && <span className={styles.error}>{error}</span>}
    </div>
  )
}
