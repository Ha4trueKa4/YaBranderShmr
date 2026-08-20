import type { GradientDefinition } from '@entities/editorDocument'

import styles from '../Properties.module.css'
import { FillProperty } from './FillProperty'

interface GradientPropertyProps {
  gradient: GradientDefinition
  onStopColorChange: (stopIndex: number, color: string) => void
}

export function GradientProperty({
  gradient,
  onStopColorChange,
}: GradientPropertyProps) {
  const [startStop, endStop] = gradient.stops

  if (!startStop || !endStop) {
    return null
  }

  return (
    <>
      <p className={styles.sectionTitle}>Linear gradient</p>
      <FillProperty
        fill={startStop.color}
        onFillChange={(color) => onStopColorChange(0, color)}
        title="Start color"
      />
      <FillProperty
        fill={endStop.color}
        onFillChange={(color) => onStopColorChange(1, color)}
        title="End color"
      />
    </>
  )
}
