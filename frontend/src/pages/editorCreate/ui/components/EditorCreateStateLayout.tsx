import iconGlow from '@shared/assets/iconGlow.svg'
import type { ReactNode } from 'react'

import styles from './EditorCreateStateLayout.module.css'

interface EditorCreateStateLayoutProps {
  composer: ReactNode
  description: string
  isCopyWidthLimited?: boolean
  title: string
}

export function EditorCreateStateLayout({
  composer,
  description,
  isCopyWidthLimited = false,
  title,
}: EditorCreateStateLayoutProps) {
  return (
    <section className={styles.state}>
      <div className={styles.preview}>
        <div
          className={isCopyWidthLimited ? styles.copyWidthLimited : styles.copy}
        >
          <img alt="" className={styles.icon} src={iconGlow} />
          <h1>{title}</h1>
          <p>{description}</p>
        </div>
      </div>
      <div className={styles.composer}>{composer}</div>
    </section>
  )
}
