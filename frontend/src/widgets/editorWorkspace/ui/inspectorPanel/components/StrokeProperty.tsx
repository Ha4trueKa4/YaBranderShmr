import type { Paint } from '@entities/editorDocument'
import { Button, Input } from '@shared/ui'

import styles from '../Properties.module.css'
import { FillProperty } from './FillProperty'

interface StrokePropertyProps {
  onAdd: () => void
  onColorChange: (color: string) => void
  onRemove: () => void
  onWidthChange: (strokeWidth: number) => void
  stroke: Paint
  strokeWidth: number
}

function getFiniteNumber(value: string): number | null {
  const number = Number(value)

  return Number.isFinite(number) ? number : null
}

export function StrokeProperty({
  onAdd,
  onColorChange,
  onRemove,
  onWidthChange,
  stroke,
  strokeWidth,
}: StrokePropertyProps) {
  const color = stroke.type === 'color' ? stroke.value : '#000000'
  const hasStroke = stroke.type !== 'none' && strokeWidth > 0

  function handleWidthChange(value: string) {
    const width = getFiniteNumber(value)

    if (width !== null) {
      onWidthChange(width)
    }
  }

  if (!hasStroke) {
    return (
      <div className={styles.field}>
        <span>Stroke</span>
        <Button onClick={onAdd} variant="secondary">
          Add outline
        </Button>
      </div>
    )
  }

  return (
    <>
      <FillProperty fill={color} onFillChange={onColorChange} title="Stroke" />
      <div className={styles.fieldGrid}>
        <div className={styles.field}>
          <span>Width</span>
          <Input
            min="0"
            onChange={(event) => handleWidthChange(event.target.value)}
            type="number"
            value={strokeWidth}
          />
        </div>
        <div className={styles.field}>
          <span>Outline</span>
          <Button onClick={onRemove} variant="secondary">
            Remove
          </Button>
        </div>
      </div>
    </>
  )
}
