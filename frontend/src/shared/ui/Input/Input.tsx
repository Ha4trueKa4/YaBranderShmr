import { forwardRef, type InputHTMLAttributes, type ReactNode } from 'react'
import styles from './Input.module.css'

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  invalid?: boolean
  trailingContent?: ReactNode
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { className, invalid = false, trailingContent, ...props },
  ref,
) {
  return (
    <span
      className={[styles.root, invalid && styles.invalid, className]
        .filter(Boolean)
        .join(' ')}
    >
      <input
        {...props}
        aria-invalid={invalid || props['aria-invalid']}
        className={styles.input}
        ref={ref}
      />
      {trailingContent && (
        <span className={styles.trailing}>{trailingContent}</span>
      )}
    </span>
  )
})
