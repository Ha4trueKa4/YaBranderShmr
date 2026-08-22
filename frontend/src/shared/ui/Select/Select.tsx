import { ChevronDown } from 'lucide-react'
import { useRef, useState } from 'react'

import { PopupMenu } from '../PopupMenu'
import styles from './Select.module.css'

export interface SelectOption {
  label: string
  value: string
}

export interface SelectProps {
  className?: string
  disabled?: boolean
  menuPlacement?: 'bottom' | 'top'
  onChange: (value: string) => void
  options: SelectOption[]
  value: string
}

export function Select({
  className,
  disabled = false,
  menuPlacement = 'bottom',
  onChange,
  options,
  value,
}: SelectProps) {
  const triggerRef = useRef<HTMLButtonElement>(null)
  const [isOpen, setIsOpen] = useState(false)
  const selectedOption = options.find((option) => option.value === value)

  return (
    <div className={[styles.root, className].filter(Boolean).join(' ')}>
      <button
        className={styles.trigger}
        disabled={disabled}
        ref={triggerRef}
        type="button"
        onClick={() => setIsOpen((open) => !open)}
      >
        <span>{selectedOption?.label ?? value}</span>
        <ChevronDown
          className={[styles.icon, isOpen && styles.iconOpen]
            .filter(Boolean)
            .join(' ')}
          size={16}
        />
      </button>
      {isOpen && (
        <PopupMenu
          anchorRef={triggerRef}
          items={options.map((option) => ({
            label: option.label,
            onClick: () => onChange(option.value),
            selected: option.value === value,
          }))}
          onClose={() => setIsOpen(false)}
          style={
            menuPlacement === 'top'
              ? { bottom: 'calc(100% + var(--space-1))', top: 'auto' }
              : undefined
          }
        />
      )}
    </div>
  )
}
