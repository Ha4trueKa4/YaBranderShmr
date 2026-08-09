import type { ShapeLayer } from '@entities/editorDocument'
import { Input } from '@shared/ui'

import styles from '../Properties.module.css'

interface ShapePropertiesProps {
  layer: ShapeLayer
}

export function ShapeProperties({ layer }: ShapePropertiesProps) {
  return (
    <div className={styles.properties}>
      {/* Подключить изменение координат, fill и stroke через feature editorEditProperties. */}
      <p className={styles.sectionTitle}>Shape</p>
      <div className={styles.fieldGrid}>
        <div className={styles.field}>
          X
          <Input readOnly value={layer.x} />
        </div>
        <div className={styles.field}>
          Y
          <Input readOnly value={layer.y} />
        </div>
      </div>
      <div className={styles.field}>
        Fill
        <Input readOnly value={layer.fill} />
      </div>
      <div className={styles.field}>
        Stroke
        <Input readOnly value={layer.stroke ?? 'None'} />
      </div>
    </div>
  )
}
