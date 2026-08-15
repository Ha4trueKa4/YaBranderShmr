import type { EditorShape, Paint } from '@entities/editorDocument'

import { FillProperty } from '../components/FillProperty'
import { PropertyField } from '../components/PropertyField'
import { StrokeProperty } from '../components/StrokeProperty'
import styles from '../Properties.module.css'

interface ShapePropertiesProps {
  layer: EditorShape
}

function paintToLabel(paint: Paint) {
  if (paint.type === 'color') {
    return paint.value
  }

  if (paint.type === 'gradient') {
    return `Gradient: ${paint.gradientId}`
  }

  return 'None'
}

export function ShapeProperties({ layer }: ShapePropertiesProps) {
  return (
    <div className={styles.properties}>
      {/* Подключить изменение координат, fill и stroke через feature editorEditProperties. */}
      <p className={styles.sectionTitle}>Shape</p>
      <div className={styles.fieldGrid}>
        <PropertyField label="X" value={layer.transform.x} />
        <PropertyField label="Y" value={layer.transform.y} />
      </div>
      <FillProperty
        fill={paintToLabel(layer.style.fill)}
        opacity={layer.style.fillOpacity}
      />
      <StrokeProperty stroke={paintToLabel(layer.style.stroke)} />
    </div>
  )
}
