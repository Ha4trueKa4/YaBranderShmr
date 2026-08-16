import { cn } from '@shared/lib'
import { ChevronLeft, ChevronRight } from 'lucide-react'

import styles from './Pagination.module.css'

export interface PaginationProps {
  className?: string
  hasNextPage: boolean
  hasPreviousPage: boolean
  isPending?: boolean
  onNextPage: () => void
  onPreviousPage: () => void
}

export function Pagination({
  className,
  hasNextPage,
  hasPreviousPage,
  isPending = false,
  onNextPage,
  onPreviousPage,
}: PaginationProps) {
  return (
    <nav className={cn(styles.pagination, className)}>
      <button
        className={styles.button}
        disabled={!hasPreviousPage || isPending}
        onClick={onPreviousPage}
        type="button"
      >
        <ChevronLeft size={16} />
        Previous
      </button>
      <button
        className={styles.button}
        disabled={!hasNextPage || isPending}
        onClick={onNextPage}
        type="button"
      >
        Next
        <ChevronRight size={16} />
      </button>
    </nav>
  )
}
