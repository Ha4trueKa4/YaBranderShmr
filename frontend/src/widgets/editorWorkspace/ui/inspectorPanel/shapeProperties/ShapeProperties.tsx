import type { ShapeLayer } from '@entities/editorDocument'

import { FillProperty } from '../components/FillProperty'
import { PropertyField } from '../components/PropertyField'
import { StrokeProperty } from '../components/StrokeProperty'
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
        <PropertyField label="X" value={layer.x} />
        <PropertyField label="Y" value={layer.y} />
      </div>
      <FillProperty fill={layer.fill} />
      <StrokeProperty stroke={layer.stroke} />
    </div>
  )
}
