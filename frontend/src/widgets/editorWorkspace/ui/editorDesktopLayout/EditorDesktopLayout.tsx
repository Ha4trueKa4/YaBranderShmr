import { cn } from '@shared/lib'
import type { ReactNode } from 'react'

import styles from './EditorDesktopLayout.module.css'

interface EditorDesktopLayoutProps {
  canEdit: boolean
  inspector: ReactNode
  layers: ReactNode
  scene: ReactNode
  status: ReactNode
  toolbar: ReactNode
}

export function EditorDesktopLayout({
  canEdit,
  inspector,
  layers,
  scene,
  status,
  toolbar,
}: EditorDesktopLayoutProps) {
  return (
    <div className={cn(styles.layout, !canEdit && styles.viewLayout)}>
      <div className={styles.toolbar}>{toolbar}</div>
      {canEdit && <aside className={styles.layers}>{layers}</aside>}
      <div className={styles.scene}>{scene}</div>
      {canEdit && <aside className={styles.inspector}>{inspector}</aside>}
      <div className={styles.status}>{status}</div>
    </div>
  )
}
