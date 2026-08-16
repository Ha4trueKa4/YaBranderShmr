import { cn } from '@shared/lib'
import type { ReactNode } from 'react'

import { ErrorState } from '../ErrorState'
import { Spinner } from '../Spinner'

import styles from './AsyncState.module.css'

export interface AsyncStateProps {
  children: ReactNode
  errorMessage?: string
  isError: boolean
  isPending: boolean
  loadingClassName?: string
}

export function AsyncState({
  children,
  errorMessage,
  isError,
  isPending,
  loadingClassName,
}: AsyncStateProps) {
  if (isPending) {
    return (
      <div className={cn(styles.loadingState, loadingClassName)}>
        <Spinner />
      </div>
    )
  }

  if (isError) {
    return <ErrorState message={errorMessage} />
  }

  return children
}
