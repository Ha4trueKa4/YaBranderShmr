import type { GradientDefinition } from '@entities/editorDocument'

import styles from '../Properties.module.css'
import { FillProperty } from './FillProperty'

interface GradientPropertyProps {
  gradient: GradientDefinition
  onEyeDropperEnd?: () => void
  onEyeDropperStart?: () => void
  onStopColorChange: (stopIndex: number, color: string) => void
}

export function GradientProperty({
  gradient,
  onEyeDropperEnd,
  onEyeDropperStart,
  onStopColorChange,
}: GradientPropertyProps) {
  const title =
    gradient.type === 'linearGradient' ? 'Linear gradient' : 'Radial gradient'

  return (
    <>
      <p className={styles.sectionTitle}>{title}</p>
      {gradient.stops.map((stop, index) => (
        <FillProperty
          fill={stop.color}
          key={stop.offset}
          onFillChange={(color) => onStopColorChange(index, color)}
          onEyeDropperEnd={onEyeDropperEnd}
          onEyeDropperStart={onEyeDropperStart}
          title={`Color ${index + 1} · ${stop.offset}`}
        />
      ))}
    </>
  )
}
