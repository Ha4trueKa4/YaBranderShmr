import { Input } from '@shared/ui'

import styles from '../Properties.module.css'

interface PropertyFieldProps {
  label: string
  value: number | string
}

export function PropertyField({ label, value }: PropertyFieldProps) {
  return (
    <div className={styles.field}>
      {label}
      <Input readOnly value={value} />
    </div>
  )
}
