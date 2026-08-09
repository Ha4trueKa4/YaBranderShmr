import type { TextLayer } from '@entities/editorDocument'
import { Input } from '@shared/ui'

import styles from '../Properties.module.css'

interface TextPropertiesProps {
  layer: TextLayer
}

export function TextProperties({ layer }: TextPropertiesProps) {
  return (
    <div className={styles.properties}>
      {/* Подключить изменение текста, шрифта и размера через feature editorEditText. */}
      <p className={styles.sectionTitle}>Text</p>
      <div className={styles.field}>
        Content
        <Input readOnly value={layer.text} />
      </div>
      <div className={styles.field}>
        Font family
        <Input readOnly value={layer.fontFamily} />
      </div>
      <div className={styles.field}>
        Font size
        <Input readOnly value={layer.fontSize} />
      </div>
    </div>
  )
}
