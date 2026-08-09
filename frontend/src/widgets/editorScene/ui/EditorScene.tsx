import { SceneFrame } from './sceneFrame/SceneFrame'
import styles from './EditorScene.module.css'

export function EditorScene() {
  return (
    <section className={styles.scene}>
      {/* Сцена читает document и viewport из editorStore; SceneFrame отвечает только за артборд. */}
      <SceneFrame />
    </section>
  )
}
