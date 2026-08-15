import { useState } from 'react'

import styles from './ProjectPreview.module.css'

interface ProjectPreviewProps {
  imageUrl?: string | null
  title: string
}

export function ProjectPreview({ imageUrl, title }: ProjectPreviewProps) {
  const [failedImageUrl, setFailedImageUrl] = useState<string | null>(null)
  const shouldShowImage = imageUrl && failedImageUrl !== imageUrl

  return (
    <div className={styles.preview}>
      {shouldShowImage ? (
        <img
          alt=""
          className={styles.image}
          onError={() => setFailedImageUrl(imageUrl)}
          src={imageUrl}
        />
      ) : (
        <span className={styles.mark} title={title} />
      )}
    </div>
  )
}
