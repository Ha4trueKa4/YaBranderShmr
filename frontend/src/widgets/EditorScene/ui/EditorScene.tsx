import { SceneFrame } from './sceneFrame/SceneFrame'
import styles from './EditorScene.module.css'

export function EditorScene() {
  return (
    <section className={styles.scene}>
      <SceneFrame />
    </section>
  )
}
