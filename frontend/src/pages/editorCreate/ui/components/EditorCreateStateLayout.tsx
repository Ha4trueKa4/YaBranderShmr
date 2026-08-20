import { GenerateIcon } from '@shared/ui'
import type { ReactNode } from 'react'

import styles from './EditorCreateStateLayout.module.css'

interface EditorCreateStateLayoutProps {
  composer: ReactNode
  description: string
  isCopyWidthLimited?: boolean
  isPreviewCompact?: boolean
  title: string
}

export function EditorCreateStateLayout({
  composer,
  description,
  isCopyWidthLimited = false,
  isPreviewCompact = false,
  title,
}: EditorCreateStateLayoutProps) {
  return (
    <section className={styles.state}>
      <div
        className={[styles.preview, isPreviewCompact && styles.previewCompact]
          .filter(Boolean)
          .join(' ')}
      >
        <div
          className={isCopyWidthLimited ? styles.copyWidthLimited : styles.copy}
        >
          <GenerateIcon className={styles.icon} />
          <h1>{title}</h1>
          <p>{description}</p>
        </div>
      </div>
      <div className={styles.composer}>{composer}</div>
    </section>
  )
}
