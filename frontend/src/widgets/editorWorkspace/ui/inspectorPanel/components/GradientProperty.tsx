import type { GradientDefinition } from '@entities/editorDocument'
import { useTranslation } from 'react-i18next'

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
  const { t } = useTranslation()
  const title =
    gradient.type === 'linearGradient'
      ? t('Linear gradient')
      : t('Radial gradient')

  return (
    <>
      <p className={styles.sectionTitle}>{title}</p>
      {gradient.stops.map((stop, index) => (
        <FillProperty
          fill={stop.color}
          key={stop.offset}
          onFillChange={(color) => onStopColorChange(index, color)}
          title={t('Color stop {{index}} · {{offset}}', {
            index: index + 1,
            offset: stop.offset,
          })}
        />
      ))}
    </>
  )
}
