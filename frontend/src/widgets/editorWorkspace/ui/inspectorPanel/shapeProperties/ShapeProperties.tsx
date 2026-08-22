import type { EditorDefinitions, EditorShape } from '@entities/editorDocument'
import { observer } from 'mobx-react-lite'
import { useTranslation } from 'react-i18next'

import { FillProperty } from '../components/FillProperty'
import { GradientProperty } from '../components/GradientProperty'
import { OpacityProperty } from '../components/OpacityProperty'
import { PropertyField } from '../components/PropertyField'
import { ScrubbableNumberInput } from '../components/ScrubbableNumberInput'
import { StrokeProperty } from '../components/StrokeProperty'
import { TransformProperties } from '../components/TransformProperties'
import styles from '../Properties.module.css'

interface ShapePropertiesProps {
  definitions?: EditorDefinitions
  layer: EditorShape
  measuredRotationCenter?: { x: number; y: number }
  onFillChange: (color: string) => void
  onEyeDropperEnd?: () => void
  onEyeDropperStart?: () => void
  onFillOpacityChange: (opacity: number) => void
  onGradientStopColorChange: (
    gradientId: string,
    stopIndex: number,
    color: string,
  ) => void
  onOpacityChange: (opacity: number) => void
  onPositionChange: (x: number, y: number) => void
  onRotationChange: (
    rotation: number,
    rotationCenter: { x: number; y: number },
  ) => void
  onStrokeAdd: () => void
  onStrokeColorChange: (color: string) => void
  onStrokeRemove: () => void
  onStrokeWidthChange: (strokeWidth: number) => void
  onTransformChange: (transform: {
    rotationCenter?: { x: number; y: number }
    scaleX: number
    scaleY: number
    x: number
    y: number
  }) => void
}

export const ShapeProperties = observer(function ShapeProperties({
  definitions,
  layer,
  measuredRotationCenter,
  onFillChange,
  onEyeDropperEnd,
  onEyeDropperStart,
  onFillOpacityChange,
  onGradientStopColorChange,
  onPositionChange,
  onOpacityChange,
  onRotationChange,
  onStrokeAdd,
  onStrokeColorChange,
  onStrokeRemove,
  onStrokeWidthChange,
  onTransformChange,
}: ShapePropertiesProps) {
  const fill = layer.style.fill
  const gradient =
    fill.type === 'gradient'
      ? definitions?.gradients.find((item) => item.id === fill.gradientId)
      : undefined
  const isEditableGradient = Boolean(gradient && gradient.stops.length > 0)
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
      <TransformProperties
        layer={layer}
        measuredRotationCenter={measuredRotationCenter}
        onRotationChange={onRotationChange}
        onTransformChange={onTransformChange}
      />
      <OpacityProperty
        onOpacityChange={onOpacityChange}
        opacity={layer.opacity}
      />
      {isEditableGradient && gradient ? (
        <GradientProperty
          gradient={gradient}
          onStopColorChange={(stopIndex, color) =>
            onGradientStopColorChange(gradient.id, stopIndex, color)
          }
          onEyeDropperEnd={onEyeDropperEnd}
          onEyeDropperStart={onEyeDropperStart}
        />
      ) : fill.type === 'gradient' ? (
        <PropertyField
          label={t('Fill')}
          labelClassName={styles.sectionTitle}
          value={t('Gradient has no editable color stops')}
        />
      ) : fill.type === 'none' ? (
        <PropertyField
          label={t('Fill')}
          labelClassName={styles.sectionTitle}
          value={t('No fill')}
        />
      ) : (
        <FillProperty
          fill={fill.value}
          onFillChange={onFillChange}
          onEyeDropperEnd={onEyeDropperEnd}
          onEyeDropperStart={onEyeDropperStart}
          onOpacityChange={onFillOpacityChange}
          opacity={layer.style.fillOpacity}
        />
      )}
      <StrokeProperty
        onAdd={onStrokeAdd}
        onColorChange={onStrokeColorChange}
        onEyeDropperEnd={onEyeDropperEnd}
        onEyeDropperStart={onEyeDropperStart}
        onRemove={onStrokeRemove}
        onWidthChange={onStrokeWidthChange}
        stroke={layer.style.stroke}
        strokeWidth={layer.style.strokeWidth}
      />
    </div>
  )
})
