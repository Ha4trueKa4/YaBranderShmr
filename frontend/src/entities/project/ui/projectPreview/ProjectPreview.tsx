import styles from './ProjectPreview.module.css'

interface ProjectPreviewProps {
  imageUrl?: string | null
  title: string
}

export function ProjectPreview({ imageUrl, title }: ProjectPreviewProps) {
  return (
    <div className={styles.preview}>
      {imageUrl ? (
        <img alt="" className={styles.image} src={imageUrl} />
      ) : (
        <span className={styles.mark} title={title} />
      )}
    </div>
  )
}
