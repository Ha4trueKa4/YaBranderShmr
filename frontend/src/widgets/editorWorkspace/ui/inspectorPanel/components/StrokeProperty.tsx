import type { Paint } from '@entities/editorDocument'
import { Button, Input } from '@shared/ui'
import { useTranslation } from 'react-i18next'
import styles from '../Properties.module.css'
import { FillProperty } from './FillProperty'
import { OpacityProperty } from './OpacityProperty'

interface StrokePropertyProps {
  onAdd: () => void
  onColorChange: (color: string) => void
  onEyeDropperEnd?: () => void
  onEyeDropperStart?: () => void
  onRemove: () => void
  onOpacityChange: (opacity: number) => void
  onWidthChange: (strokeWidth: number) => void
  stroke: Paint
  strokeOpacity: number
  strokeWidth: number
}

function getFiniteNumber(value: string): number | null {
  const number = Number(value)

  return Number.isFinite(number) ? number : null
}

export function StrokeProperty({
  onAdd,
  onColorChange,
  onEyeDropperEnd,
  onEyeDropperStart,
  onRemove,
  onOpacityChange,
  onWidthChange,
  stroke,
  strokeOpacity,
  strokeWidth,
}: StrokePropertyProps) {
  const { t } = useTranslation()
  const color = stroke.type === 'color' ? stroke.value : '#000000'
  const hasStroke = stroke.type !== 'none' && strokeWidth > 0

  function handleWidthChange(value: string) {
    if (value.trim() === '') {
      return
    }

    const width = getFiniteNumber(value)

    if (width !== null) {
      onWidthChange(width)
    }
  }

  if (!hasStroke) {
    return (
      <div className={styles.field}>
        <span>{t('Stroke')}</span>
        <Button onClick={onAdd} size="sm" variant="secondary">
          {t('Add outline')}
        </Button>
      </div>
    )
  }

  return (
    <>
      <FillProperty
        fill={color}
        onFillChange={onColorChange}
        onEyeDropperEnd={onEyeDropperEnd}
        onEyeDropperStart={onEyeDropperStart}
        title={t('Stroke')}
      />
      <OpacityProperty
        label="Stroke opacity"
        onOpacityChange={onOpacityChange}
        opacity={strokeOpacity}
      />
      <div className={styles.fieldGrid}>
        <div className={styles.field}>
          <span>{t('Width')}</span>
          <Input
            min="0"
            onChange={(event) => handleWidthChange(event.target.value)}
            type="number"
            value={strokeWidth}
          />
        </div>
        <div className={styles.field}>
          <span>{t('Outline')}</span>
          <Button onClick={onRemove} size="sm" variant="secondary">
            {t('Remove')}
          </Button>
        </div>
      </div>
    </>
  )
}
