import type { EditorShape, Paint } from '@entities/editorDocument'
import { observer } from 'mobx-react-lite'

import { FillProperty } from '../components/FillProperty'
import { PropertyField } from '../components/PropertyField'
import { StrokeProperty } from '../components/StrokeProperty'
import styles from '../Properties.module.css'

interface ShapePropertiesProps {
  layer: EditorShape
  onFillChange: (color: string) => void
  onFillOpacityChange: (opacity: number) => void
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

export const ShapeProperties = observer(function ShapeProperties({
  layer,
  onFillChange,
  onFillOpacityChange,
}: ShapePropertiesProps) {
  return (
    <div className={styles.properties}>
      {/* Подключить изменение координат, fill и stroke через feature editorEditProperties. */}
      <p className={styles.sectionTitle}>Shape</p>
      <div className={styles.fieldGrid}>
        <PropertyField label="X" value={layer.transform.x} />
        <PropertyField label="Y" value={layer.transform.y} />
      </div>
      <FillProperty
        fill={
          layer.style.fill.type === 'color' ? layer.style.fill.value : '#000000'
        }
        onFillChange={onFillChange}
        onOpacityChange={onFillOpacityChange}
        opacity={layer.style.fillOpacity}
      />
      <StrokeProperty stroke={paintToLabel(layer.style.stroke)} />
    </div>
  )
})
