import type { ReactNode } from 'react'

import { ErrorState } from '../ErrorState'
import { Spinner } from '../Spinner'

import styles from './AsyncState.module.css'

export interface AsyncStateProps {
  children: ReactNode
  errorMessage?: string
  isError: boolean
  isPending: boolean
}

export function AsyncState({
  children,
  errorMessage,
  isError,
  isPending,
}: AsyncStateProps) {
  if (isPending) {
    return (
      <div className={styles.loadingState}>
        <Spinner />
      </div>
    )
  }

  if (isError) {
    return <ErrorState message={errorMessage} />
  }

  return children
}
