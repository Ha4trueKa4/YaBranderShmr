import type { EditorShape } from '@entities/editorDocument'
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
  onStrokeAdd: () => void
  onStrokeColorChange: (color: string) => void
  onStrokeRemove: () => void
  onStrokeWidthChange: (strokeWidth: number) => void
}

export const ShapeProperties = observer(function ShapeProperties({
  layer,
  onFillChange,
  onFillOpacityChange,
  onPositionChange,
  onStrokeAdd,
  onStrokeColorChange,
  onStrokeRemove,
  onStrokeWidthChange,
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
      <StrokeProperty
        onAdd={onStrokeAdd}
        onColorChange={onStrokeColorChange}
        onRemove={onStrokeRemove}
        onWidthChange={onStrokeWidthChange}
        stroke={layer.style.stroke}
        strokeWidth={layer.style.strokeWidth}
      />
    </div>
  )
})
