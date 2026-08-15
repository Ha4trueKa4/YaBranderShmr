import { type FeedProject, FeedProjectCard } from '@entities/project'

import styles from './Feed.module.css'

export interface FeedFilters {
  scope: 'discover' | 'following'
  searchQuery: string
}

interface FeedProjectGridProps {
  filters: FeedFilters
  projects: FeedProject[]
}

export function FeedProjectGrid({ filters, projects }: FeedProjectGridProps) {
  const { searchQuery } = filters
  const normalizedQuery = searchQuery.trim().toLocaleLowerCase()
  const visibleProjects = projects.filter((project) => {
    return (
      !normalizedQuery ||
      project.name.toLocaleLowerCase().includes(normalizedQuery) ||
      project.author.name.toLocaleLowerCase().includes(normalizedQuery)
    )
  })

  return (
    <div className={styles.grid}>
      {visibleProjects.map((project) => (
        <FeedProjectCard key={project.id} project={project} />
      ))}
    </div>
  )
}
