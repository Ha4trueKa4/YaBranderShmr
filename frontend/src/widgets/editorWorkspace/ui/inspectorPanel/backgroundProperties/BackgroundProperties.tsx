import type { BackgroundLayer } from '@entities/editorDocument'

import { FillProperty } from '../components/FillProperty'
import styles from '../Properties.module.css'

interface BackgroundPropertiesProps {
  layer: BackgroundLayer
}

export function BackgroundProperties({ layer }: BackgroundPropertiesProps) {
  return (
    <div className={styles.properties}>
      {/* Подключить изменение fill через feature editorChangeBackground. */}
      <p className={styles.sectionTitle}>Background</p>
      <FillProperty fill={layer.fill} />
    </div>
  )
}
