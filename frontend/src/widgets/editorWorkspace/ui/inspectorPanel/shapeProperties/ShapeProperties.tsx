import type { EditorShape, Paint } from '@entities/editorDocument'
import { Range } from '@shared/ui'
import { observer } from 'mobx-react-lite'

import { FillProperty } from '../components/FillProperty'
import { ScrubbableNumberInput } from '../components/ScrubbableNumberInput'
import { StrokeProperty } from '../components/StrokeProperty'
import styles from '../Properties.module.css'

interface ShapePropertiesProps {
  layer: EditorShape
  onFillChange: (color: string) => void
  onFillOpacityChange: (opacity: number) => void
  onOpacityChange: (opacity: number) => void
  onPositionChange: (x: number, y: number) => void
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
  onOpacityChange,
  onPositionChange,
}: ShapePropertiesProps) {
  return (
    <div className={styles.properties}>
      <p className={styles.sectionTitle}>Shape</p>
      <div className={styles.fieldGrid}>
        <div className={styles.field}>
          <span>X</span>
          <ScrubbableNumberInput
            onValueChange={(x) => onPositionChange(x, layer.transform.y)}
            value={layer.transform.x}
          />
        </div>
        <div className={styles.field}>
          <span>Y</span>
          <ScrubbableNumberInput
            axis="vertical"
            onValueChange={(y) => onPositionChange(layer.transform.x, y)}
            value={layer.transform.y}
          />
        </div>
      </div>
      <FillProperty
        fill={
          layer.style.fill.type === 'color' ? layer.style.fill.value : '#000000'
        }
        onFillChange={onFillChange}
        onOpacityChange={onFillOpacityChange}
        opacity={layer.style.fillOpacity}
      />
      <div className={styles.field}>
        <div className={styles.rangeHeader}>
          <span>Layer opacity</span>
          <strong>{Math.round(layer.opacity * 100)}%</strong>
        </div>
        <Range
          max={100}
          min={0}
          onChange={(event) =>
            onOpacityChange(Number(event.target.value) / 100)
          }
          value={Math.round(layer.opacity * 100)}
        />
      </div>
      <StrokeProperty stroke={paintToLabel(layer.style.stroke)} />
    </div>
  )
})
