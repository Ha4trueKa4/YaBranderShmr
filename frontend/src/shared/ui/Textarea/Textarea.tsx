import type { TextareaHTMLAttributes } from 'react'
import styles from './Textarea.module.css'

export interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  invalid?: boolean
}

export function Textarea({ className, invalid = false, ...props }: TextareaProps) {
  return <textarea {...props} aria-invalid={invalid || props['aria-invalid']} className={[styles.textarea, invalid && styles.invalid, className].filter(Boolean).join(' ')} />
}
