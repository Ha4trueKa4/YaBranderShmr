import { forwardRef, type SelectHTMLAttributes } from 'react'
import styles from './Select.module.css'

export interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  invalid?: boolean
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  function Select({ className, invalid = false, ...props }, ref) {
    return (
      <select
        {...props}
        className={[styles.select, invalid && styles.invalid, className]
          .filter(Boolean)
          .join(' ')}
        ref={ref}
      />
    )
  },
)
