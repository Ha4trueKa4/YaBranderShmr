import {
  type FeedProject,
  FeedProjectCard,
  projectReferencesStore,
} from '@entities/project'
import { observer } from 'mobx-react-lite'

import styles from './Feed.module.css'

export interface FeedFilters {
  scope: 'discover' | 'following'
  searchQuery: string
}

interface FeedProjectGridProps {
  filters: FeedFilters
  projects: FeedProject[]
}

export const FeedProjectGrid = observer(function FeedProjectGrid({
  filters,
  projects,
}: FeedProjectGridProps) {
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
        <FeedProjectCard
          canAddReference={projectReferencesStore.canAddReference}
          isReferenceSelected={projectReferencesStore.has(project.id)}
          key={project.id}
          onReferenceToggle={(referenceProject) =>
            projectReferencesStore.toggle({
              id: referenceProject.id,
              name: referenceProject.name,
            })
          }
          project={project}
        />
      ))}
    </div>
  )
})
