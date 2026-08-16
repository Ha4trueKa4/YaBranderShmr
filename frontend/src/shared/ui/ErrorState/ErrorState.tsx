import { CircleAlert } from 'lucide-react'

import styles from './ErrorState.module.css'

export interface ErrorStateProps {
  message?: string
}

export function ErrorState({
  message = 'Unable to load data. Please try again.',
}: ErrorStateProps) {
  return (
    <div className={styles.state}>
      <CircleAlert className={styles.icon} size={24} />
      <p>{message}</p>
    </div>
  )
}
