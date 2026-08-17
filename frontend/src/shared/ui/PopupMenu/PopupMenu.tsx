import { type CSSProperties, type RefObject, useEffect, useRef } from 'react'

import styles from './PopupMenu.module.css'

export interface PopupMenuItem {
  label: string
  onClick: () => void
  selected?: boolean
}

export interface PopupMenuProps {
  anchorRef?: RefObject<HTMLElement | null>
  items: PopupMenuItem[]
  onClose: () => void
  style?: CSSProperties
}

export function PopupMenu({
  anchorRef,
  items,
  onClose,
  style,
}: PopupMenuProps) {
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function handlePointerDown(event: PointerEvent) {
      if (
        event.target instanceof Node &&
        !menuRef.current?.contains(event.target) &&
        !anchorRef?.current?.contains(event.target)
      ) {
        onClose()
      }
    }

    document.addEventListener('pointerdown', handlePointerDown)

    return () => document.removeEventListener('pointerdown', handlePointerDown)
  }, [anchorRef?.current?.contains, onClose])

  return (
    <div className={styles.menu} ref={menuRef} style={style}>
      {items.map((item) => (
        <button
          className={[styles.item, item.selected && styles.selected]
            .filter(Boolean)
            .join(' ')}
          key={item.label}
          onClick={(event) => {
            event.stopPropagation()
            item.onClick()
            onClose()
          }}
          type="button"
        >
          {item.label}
        </button>
      ))}
    </div>
  )
}
