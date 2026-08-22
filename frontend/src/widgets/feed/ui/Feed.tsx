import { authStore } from '@app/providers/auth'
import { useFeedInfiniteQuery } from '@entities/project'
import { getApiErrorMessage } from '@shared/api/api'
import { cn } from '@shared/lib'
import { AsyncState, Button, Input } from '@shared/ui'
import { Search, SlidersHorizontal } from 'lucide-react'
import { observer } from 'mobx-react-lite'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'

import styles from './Feed.module.css'
import { type FeedFilters, FeedProjectGrid } from './FeedProjectGrid'

export const Feed = observer(function Feed() {
  const { t } = useTranslation()
  const isAuthorized = authStore.isAuthorized
  const [scope, setScope] = useState<FeedFilters['scope']>('discover')
  const [searchQuery, setSearchQuery] = useState('')
  const {
    data,
    error,
    fetchNextPage,
    hasNextPage,
    isError,
    isFetchingNextPage,
    isFetchNextPageError,
    isPending,
  } = useFeedInfiniteQuery(scope)
  const projects = data?.pages.flatMap((page) => page.items) ?? []

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
            {t('All')}
          </button>
          {isAuthorized && (
            <button
              className={cn(
                styles.scopeTab,
                scope === 'following' && styles.active,
              )}
              onClick={() => setScope('following')}
              type="button"
            >
              {t('Following')}
            </button>
          )}
        </div>

        <Input
          className={styles.search}
          leadingContent={<Search size={16} />}
          onChange={(event) => setSearchQuery(event.target.value)}
          placeholder={t('Search projects')}
          type="search"
          value={searchQuery}
        />

        <Button className={styles.filtersButton} variant="ghost">
          <SlidersHorizontal size={16} />
          {t('Filters')}
        </Button>
      </div>

      <AsyncState
        errorMessage={getApiErrorMessage(error)}
        isError={isError}
        isPending={isPending}
      >
        <FeedProjectGrid filters={{ scope, searchQuery }} projects={projects} />

        {hasNextPage && (
          <div className={styles.loadMore}>
            {isFetchNextPageError && (
              <p className={styles.loadMoreError} role="alert">
                {getApiErrorMessage(error)}
              </p>
            )}
            <Button
              loading={isFetchingNextPage}
              onClick={() => fetchNextPage()}
              variant="secondary"
            >
              {isFetchNextPageError ? t('Retry loading') : t('Load more')}
            </Button>
          </div>
        )}
      </AsyncState>
    </section>
  )
})
