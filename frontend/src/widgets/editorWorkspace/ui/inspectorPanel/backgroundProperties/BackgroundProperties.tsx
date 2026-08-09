import type { BackgroundLayer } from '@entities/editorDocument'
import { Input } from '@shared/ui'

import styles from '../Properties.module.css'

interface BackgroundPropertiesProps {
  layer: BackgroundLayer
}

export function BackgroundProperties({ layer }: BackgroundPropertiesProps) {
  return (
    <div className={styles.properties}>
      {/* Подключить изменение fill через feature editorChangeBackground. */}
      <p className={styles.sectionTitle}>Background</p>
      <div className={styles.field}>
        Fill
        <Input readOnly value={layer.fill} />
      </div>
    </div>
  )
}
