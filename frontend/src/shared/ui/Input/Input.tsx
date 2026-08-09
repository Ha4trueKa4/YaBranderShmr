import { forwardRef, type InputHTMLAttributes, type ReactNode } from 'react'
import styles from './Input.module.css'

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  invalid?: boolean
  leadingContent?: ReactNode
  trailingContent?: ReactNode
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { className, invalid = false, leadingContent, trailingContent, ...props },
  ref,
) {
  return (
    <span
      className={[styles.root, invalid && styles.invalid, className]
        .filter(Boolean)
        .join(' ')}
    >
      {leadingContent && (
        <span className={styles.leading}>{leadingContent}</span>
      )}
      <input {...props} className={styles.input} ref={ref} />
      {trailingContent && (
        <span className={styles.trailing}>{trailingContent}</span>
      )}
    </span>
  )
})
