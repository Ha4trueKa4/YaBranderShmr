import { cn } from '@shared/lib'
import { Button, Input } from '@shared/ui'
import { Search, SlidersHorizontal } from 'lucide-react'
import { useState } from 'react'

import styles from './Feed.module.css'
import { type FeedFilters, FeedProjectGrid } from './FeedProjectGrid'

export function Feed() {
  const [scope, setScope] = useState<FeedFilters['scope']>('all')
  const [searchQuery, setSearchQuery] = useState('')

  return (
    <section className={styles.feed}>
      <div className={styles.toolbar}>
        <div className={styles.scopeTabs}>
          <button
            className={cn(styles.scopeTab, scope === 'all' && styles.active)}
            onClick={() => setScope('all')}
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

      <FeedProjectGrid filters={{ scope, searchQuery }} />
    </section>
  )
}
