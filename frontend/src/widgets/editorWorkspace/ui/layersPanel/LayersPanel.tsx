import styles from './LayersPanel.module.css'

export function LayersPanel() {
  return (
    <section className={styles.panel}>
      {/* Рендерить дерево document.layers и вызывать feature выбора слоя по клику. */}
      <h2>Layers</h2>
      <p>The layer tree will appear here.</p>
    </section>
  )
}
