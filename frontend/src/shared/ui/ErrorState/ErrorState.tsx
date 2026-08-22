import { CircleAlert } from 'lucide-react'
import { useTranslation } from 'react-i18next'

import styles from './ErrorState.module.css'

export interface ErrorStateProps {
  message?: string
}

export function ErrorState({ message }: ErrorStateProps) {
  const { t } = useTranslation()

  return (
    <div className={styles.state}>
      <CircleAlert className={styles.icon} size={24} />
      <p>{message ?? t('Unable to load data. Please try again.')}</p>
    </div>
  )
}
