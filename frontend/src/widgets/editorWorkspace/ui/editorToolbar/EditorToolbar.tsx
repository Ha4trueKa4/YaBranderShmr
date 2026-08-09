import styles from './EditorToolbar.module.css'

export function EditorToolbar() {
  return (
    <header className={styles.toolbar}>
      {/* Здесь появятся инструменты Select/Move, Undo/Redo, Save version и Export. */}
      <strong className={styles.brand}>Brandkit AI</strong>
      <span className={styles.projectName}>Untitled project</span>
      <span className={styles.actions}>Tools and export controls</span>
    </header>
  )
}
