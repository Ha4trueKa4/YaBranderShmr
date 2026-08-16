import styles from '../Properties.module.css'
import { PropertyField } from './PropertyField'

interface StrokePropertyProps {
  stroke?: string
}

export function StrokeProperty({ stroke }: StrokePropertyProps) {
  return (
    <PropertyField
      label="Stroke"
      labelClassName={styles.sectionTitle}
      value={stroke ?? 'None'}
    />
  )
}
