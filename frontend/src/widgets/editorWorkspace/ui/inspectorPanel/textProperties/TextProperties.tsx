import type { TextLayer } from '@entities/editorDocument'

import { PropertyField } from '../components/PropertyField'
import styles from '../Properties.module.css'

interface TextPropertiesProps {
  layer: TextLayer
}

export function TextProperties({ layer }: TextPropertiesProps) {
  return (
    <div className={styles.properties}>
      {/* Подключить изменение текста, шрифта и размера через feature editorEditText. */}
      <p className={styles.sectionTitle}>Text</p>
      <PropertyField label="Content" value={layer.text} />
      <PropertyField label="Font family" value={layer.fontFamily} />
      <PropertyField label="Font size" value={layer.fontSize} />
    </div>
  )
}
