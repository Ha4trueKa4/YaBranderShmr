import styles from './ProjectPreview.module.css'

interface ProjectPreviewProps {
  title: string
}

export function ProjectPreview({ title }: ProjectPreviewProps) {
  return (
    <div className={styles.preview}>
      <span className={styles.mark} title={title} />
    </div>
  )
}
