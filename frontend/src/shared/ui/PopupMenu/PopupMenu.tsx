import { useEffect, useRef } from 'react'

import styles from './PopupMenu.module.css'

export interface PopupMenuItem {
  label: string
  onClick: () => void
}

export interface PopupMenuProps {
  items: PopupMenuItem[]
  onClose: () => void
}

export function PopupMenu({ items, onClose }: PopupMenuProps) {
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function handlePointerDown(event: PointerEvent) {
      if (
        event.target instanceof Node &&
        !menuRef.current?.contains(event.target)
      ) {
        onClose()
      }
    }

    document.addEventListener('pointerdown', handlePointerDown)

    return () => document.removeEventListener('pointerdown', handlePointerDown)
  }, [onClose])

  return (
    <div className={styles.menu} ref={menuRef}>
      {items.map((item) => (
        <button
          className={styles.item}
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
