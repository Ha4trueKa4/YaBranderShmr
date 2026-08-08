import type { ReactNode } from 'react'
import styles from './FormField.module.css'

export interface FormFieldProps {
  children: ReactNode
  error?: string
  label: string
  required?: boolean
}

export function FormField({
  children,
  error,
  label,
  required = false,
}: FormFieldProps) {
  return (
    <div className={styles.field}>
      <span className={styles.label}>
        {label}
        {required && <span className={styles.required}> *</span>}
      </span>
      {children}
      {error && <span className={styles.error}>{error}</span>}
    </div>
  )
}
