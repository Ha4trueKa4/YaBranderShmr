import styles from './EditorStatusBar.module.css'

export function EditorStatusBar() {
  // Подставить selectedLayer, размеры артборда и zoom из editorStore.
  return <footer className={styles.status}>No layer selected · 100%</footer>
}
