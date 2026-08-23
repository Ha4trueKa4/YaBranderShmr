import { Range } from '@shared/ui'
import { useTranslation } from 'react-i18next'

import styles from './FillProperty.module.css'

interface OpacityPropertyProps {
  label?: string
  onOpacityChange: (opacity: number) => void
  opacity: number
}

export function OpacityProperty({
  label = 'Opacity',
  onOpacityChange,
  opacity,
}: OpacityPropertyProps) {
  const { t } = useTranslation()
  const opacityPercent = Math.round(opacity * 100)

  return (
    <>
      <div className={styles.opacityHeader}>
        <span>{t(label)}</span>
        <strong>{opacityPercent}%</strong>
      </div>
      <Range
        max="100"
        min="0"
        onChange={(event) => onOpacityChange(Number(event.target.value) / 100)}
        value={opacityPercent}
      />
    </>
  )
}
