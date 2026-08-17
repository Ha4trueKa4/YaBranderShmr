import { forwardRef, type ReactNode, type TextareaHTMLAttributes } from 'react'
import styles from './Textarea.module.css'

export interface TextareaProps
  extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  invalid?: boolean
  leadingContent?: ReactNode
  resizable?: boolean
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  function Textarea(
    { className, invalid = false, leadingContent, resizable = true, ...props },
    ref,
  ) {
    return (
      <span className={[styles.root, className].filter(Boolean).join(' ')}>
        {leadingContent && (
          <span className={styles.leading}>{leadingContent}</span>
        )}
        <textarea
          {...props}
          className={[
            styles.textarea,
            leadingContent && styles.withLeading,
            invalid && styles.invalid,
            !resizable && styles.notResizable,
          ]
            .filter(Boolean)
            .join(' ')}
          ref={ref}
        />
      </span>
    )
  },
)
