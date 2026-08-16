import { Input } from '@shared/ui'

import styles from '../Properties.module.css'

interface PropertyFieldProps {
  label: string
  labelClassName?: string
  value: number | string
}

export function PropertyField({
  label,
  labelClassName,
  value,
}: PropertyFieldProps) {
  return (
    <div className={styles.field}>
      <span className={labelClassName}>{label}</span>
      <Input readOnly value={value} />
    </div>
  )
}
