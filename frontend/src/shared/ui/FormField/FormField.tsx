import { useId, type ReactNode } from 'react'
import styles from './FormField.module.css'

export interface FormFieldProps {
  children: ReactNode
  className?: string
  description?: string
  error?: string
  label: string
  required?: boolean
}

export function FormField({ children, className, description, error, label, required = false }: FormFieldProps) {
  const descriptionId = useId()
  const errorId = useId()

  return (
    <label className={[styles.field, className].filter(Boolean).join(' ')}>
      <span className={styles.label}>{label}{required && <span aria-hidden="true" className={styles.required}> *</span>}</span>
      {children}
      {error ? <span className={styles.error} id={errorId} role="alert">{error}</span> : description && <span className={styles.description} id={descriptionId}>{description}</span>}
    </label>
  )
}
