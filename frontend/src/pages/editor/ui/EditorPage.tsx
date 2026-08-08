
import { EditorHeader } from '@widgets'
import styles from './EditorPage.module.css'

export function EditorPage() {
  return (
    <main className={styles.page}>
      <EditorHeader />
      <section className={styles.content}>
        <p className={styles.eyebrow}>Brandkit AI</p>
        <h1>Редактор</h1>
        <p>Страница редактора пока в разработке.</p>
      </section>
    </main>
  )
}
