import { useFeedQuery } from '@entities/project'
import { getApiErrorMessage } from '@shared/api/api'
import { cn } from '@shared/lib'
import { Button, ErrorState, Input, Spinner } from '@shared/ui'
import { Search, SlidersHorizontal } from 'lucide-react'
import { useState } from 'react'

import styles from './Feed.module.css'
import { type FeedFilters, FeedProjectGrid } from './FeedProjectGrid'

export function Feed() {
  const [scope, setScope] = useState<FeedFilters['scope']>('discover')
  const [searchQuery, setSearchQuery] = useState('')
  const { data, error, isError, isPending } = useFeedQuery(scope)

  return (
    <section className={styles.feed}>
      <div className={styles.toolbar}>
        <div className={styles.scopeTabs}>
          <button
            className={cn(
              styles.scopeTab,
              scope === 'discover' && styles.active,
            )}
            onClick={() => setScope('discover')}
            type="button"
          >
            All
          </button>
          <button
            className={cn(
              styles.scopeTab,
              scope === 'following' && styles.active,
            )}
            onClick={() => setScope('following')}
            type="button"
          >
            Following
          </button>
        </div>

        <Input
          className={styles.search}
          leadingContent={<Search size={16} />}
          onChange={(event) => setSearchQuery(event.target.value)}
          placeholder="Search projects"
          type="search"
          value={searchQuery}
        />

        <Button className={styles.filtersButton} variant="ghost">
          <SlidersHorizontal size={16} />
          Filters
        </Button>
      </div>

      {isPending ? (
        <div className={styles.loadingState}>
          <Spinner />
        </div>
      ) : isError ? (
        <ErrorState message={getApiErrorMessage(error)} />
      ) : (
        <FeedProjectGrid
          filters={{ scope, searchQuery }}
          projects={data?.items ?? []}
        />
      )}
    </section>
  )
}
