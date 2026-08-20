import type { EditorDefinitions, EditorShape } from '@entities/editorDocument'
import { observer } from 'mobx-react-lite'
import { useTranslation } from 'react-i18next'

import { FillProperty } from '../components/FillProperty'
import { GradientProperty } from '../components/GradientProperty'
import { PropertyField } from '../components/PropertyField'
import { ScrubbableNumberInput } from '../components/ScrubbableNumberInput'
import { StrokeProperty } from '../components/StrokeProperty'
import styles from '../Properties.module.css'

interface ShapePropertiesProps {
  definitions?: EditorDefinitions
  layer: EditorShape
  onFillChange: (color: string) => void
  onFillOpacityChange: (opacity: number) => void
  onGradientStopColorChange: (
    gradientId: string,
    stopIndex: number,
    color: string,
  ) => void
  onOpacityChange: (opacity: number) => void
  onPositionChange: (x: number, y: number) => void
  onStrokeAdd: () => void
  onStrokeColorChange: (color: string) => void
  onStrokeRemove: () => void
  onStrokeWidthChange: (strokeWidth: number) => void
}

export const ShapeProperties = observer(function ShapeProperties({
  definitions,
  layer,
  onFillChange,
  onFillOpacityChange,
  onGradientStopColorChange,
  onPositionChange,
  onStrokeAdd,
  onStrokeColorChange,
  onStrokeRemove,
  onStrokeWidthChange,
}: ShapePropertiesProps) {
  const fill = layer.style.fill
  const gradient =
    fill.type === 'gradient'
      ? definitions?.gradients.find((item) => item.id === fill.gradientId)
      : undefined
  const isEditableGradient =
    gradient?.type === 'linearGradient' && gradient.stops.length === 2
  const { t } = useTranslation()
  return (
    <div className={styles.properties}>
      <p className={styles.sectionTitle}>{t('Shape')}</p>
      <div className={styles.fieldGrid}>
        <div className={styles.field}>
          <span>{t('X')}</span>
          <ScrubbableNumberInput
            onValueChange={(x) => onPositionChange(x, layer.transform.y)}
            value={layer.transform.x}
          />
        </div>
        <div className={styles.field}>
          <span>{t('Y')}</span>
          <ScrubbableNumberInput
            axis="vertical"
            onValueChange={(y) => onPositionChange(layer.transform.x, y)}
            value={layer.transform.y}
          />
        </div>
      </div>
      {isEditableGradient && gradient ? (
        <GradientProperty
          gradient={gradient}
          onStopColorChange={(stopIndex, color) =>
            onGradientStopColorChange(gradient.id, stopIndex, color)
          }
        />
      ) : fill.type === 'gradient' ? (
        <PropertyField
          label={t('Fill')}
          labelClassName={styles.sectionTitle}
          value={t('Gradient editing supports two-stop linear gradients')}
        />
      ) : (
        <FillProperty
          fill={fill.type === 'color' ? fill.value : '#000000'}
          onFillChange={onFillChange}
          onOpacityChange={onFillOpacityChange}
          opacity={layer.style.fillOpacity}
        />
      )}
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
