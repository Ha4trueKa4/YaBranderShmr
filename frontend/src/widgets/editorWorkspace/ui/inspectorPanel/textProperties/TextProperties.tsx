import type { EditorText } from '@entities/editorDocument'

import { PropertyField } from '../components/PropertyField'
import styles from '../Properties.module.css'

interface TextPropertiesProps {
  layer: EditorText
}

export function TextProperties({ layer }: TextPropertiesProps) {
  return (
    <div className={styles.properties}>
      {/* Подключить изменение текста, шрифта и размера через feature editorEditText. */}
      <p className={styles.sectionTitle}>Text</p>
      <PropertyField label="Content" value={layer.text} />
      <PropertyField label="Font family" value={layer.style.fontFamily} />
      <PropertyField label="Font size" value={layer.style.fontSize} />
    </div>
  )
}
