import { PopupMenu, Portal } from '@shared/ui'
import { ChevronDown } from 'lucide-react'
import { type CSSProperties, useRef, useState } from 'react'

import { useModelsQuery } from '../../api/queries'
import styles from './ModelSelect.module.css'

export const DEFAULT_GENERATION_MODEL = 'deepseek-v4-flash/latest'

const menuGap = 4
const menuMaxHeight = 320
const viewportPadding = 16

interface ModelSelectProps {
  disabled?: boolean
  onChange: (model: string) => void
  onUnauthorizedSelect?: () => void
  value: string
  variant?: 'default' | 'compact'
}

export function ModelSelect({
  disabled = false,
  onChange,
  onUnauthorizedSelect,
  value,
  variant = 'default',
}: ModelSelectProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [menuPosition, setMenuPosition] = useState<CSSProperties>({})
  const triggerRef = useRef<HTMLButtonElement>(null)
  const { data: models = [], isError, isLoading } = useModelsQuery()
  const options = models.length
    ? models
    : [
        {
          id: DEFAULT_GENERATION_MODEL,
          owner: 'DeepSeek',
        },
      ]

  const selectedModel =
    options.find((model) => model.id === value) ?? options[0]
  const isDisabled = disabled || isLoading

  function toggleMenu() {
    if (isError && onUnauthorizedSelect) {
      onUnauthorizedSelect?.()

      return
    }

    if (isOpen) {
      setIsOpen(false)

      return
    }

    const triggerBounds = triggerRef.current?.getBoundingClientRect()

    if (triggerBounds) {
      const isMobileViewport = window.matchMedia('(width < 720px)').matches

      if (isMobileViewport) {
        setMenuPosition({
          bottom: viewportPadding,
          left: viewportPadding,
          maxHeight: `min(${menuMaxHeight}px, calc(100dvh - ${viewportPadding * 2}px))`,
          position: 'fixed',
          right: viewportPadding,
          top: 'auto',
          transform: 'none',
        })
        setIsOpen(true)

        return
      }

      const availableSpaceAbove = triggerBounds.top - viewportPadding
      const availableSpaceBelow =
        window.innerHeight - triggerBounds.bottom - viewportPadding
      const opensUpward = availableSpaceAbove >= availableSpaceBelow
      const availableSpace = opensUpward
        ? availableSpaceAbove
        : availableSpaceBelow

      setMenuPosition({
        maxHeight: Math.min(menuMaxHeight, Math.max(0, availableSpace)),
        right: window.innerWidth - triggerBounds.right,
        top: opensUpward
          ? triggerBounds.top - menuGap
          : triggerBounds.bottom + menuGap,
        transform: opensUpward ? 'translateY(-100%)' : undefined,
      })
    }

    setIsOpen(true)
  }

  return (
    <div
      className={[styles.root, styles[variant]].filter(Boolean).join(' ')}
      onPointerDown={(event) => event.stopPropagation()}
    >
      <button
        className={styles.trigger}
        disabled={isDisabled}
        onClick={toggleMenu}
        ref={triggerRef}
        type="button"
      >
        <span className={styles.value}>
          {selectedModel.owner} — {selectedModel.id}
        </span>
        <ChevronDown
          className={[styles.chevron, isOpen && styles.chevronOpen]
            .filter(Boolean)
            .join(' ')}
          size={16}
        />
      </button>
      {isOpen && (
        <Portal>
          <PopupMenu
            anchorRef={triggerRef}
            items={options.map((model) => ({
              label: `${model.owner} — ${model.id}`,
              onClick: () => onChange(model.id),
              selected: model.id === selectedModel.id,
            }))}
            onClose={() => setIsOpen(false)}
            style={menuPosition}
          />
        </Portal>
      )}
    </div>
  )
}
