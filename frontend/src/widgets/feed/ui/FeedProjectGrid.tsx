import { type FeedProject, FeedProjectCard } from '@entities/project'

import styles from './Feed.module.css'

export interface FeedFilters {
  scope: 'all' | 'following'
  searchQuery: string
}

interface FeedProjectGridProps {
  filters: FeedFilters
}

const projects: FeedProject[] = [
  {
    author: { avatarUrl: null, id: 'alex-rivera', name: 'Alex Rivera' },
    id: 'orion-labs',
    publishedAt: '2d ago',
    name: 'Orion Labs',
    previewUrl: null,
  },
  {
    author: { avatarUrl: null, id: 'mia-chen', name: 'Mia Chen' },
    id: 'nova-studio',
    publishedAt: '2d ago',
    name: 'Nova Studio',
    previewUrl: null,
  },
  {
    author: { avatarUrl: null, id: 'noah-davis', name: 'Noah Davis' },
    id: 'kite-finance',
    publishedAt: '2d ago',
    name: 'Kite Finance',
    previewUrl: null,
  },
  {
    author: { avatarUrl: null, id: 'dana-park', name: 'Dana Park' },
    id: 'field-notes',
    publishedAt: '2d ago',
    name: 'Field Notes',
    previewUrl: null,
  },
  {
    author: { avatarUrl: null, id: 'leo-martin', name: 'Leo Martin' },
    id: 'pulse-summit',
    publishedAt: '2d ago',
    name: 'Pulse Summit',
    previewUrl: null,
  },
  {
    author: { avatarUrl: null, id: 'maya-singh', name: 'Maya Singh' },
    id: 'echo-market',
    publishedAt: '2d ago',
    name: 'Echo Market',
    previewUrl: null,
  },
  {
    author: { avatarUrl: null, id: 'jules-kim', name: 'Jules Kim' },
    id: 'moss-and-co',
    publishedAt: '2d ago',
    name: 'Moss & Co',
    previewUrl: null,
  },
  {
    author: { avatarUrl: null, id: 'nina-cole', name: 'Nina Cole' },
    id: 'form-and-field',
    publishedAt: '2d ago',
    name: 'Form & Field',
    previewUrl: null,
  },
]

const followingAuthorIds = new Set([
  'alex-rivera',
  'jules-kim',
  'leo-martin',
  'noah-davis',
])

export function FeedProjectGrid({ filters }: FeedProjectGridProps) {
  const { scope, searchQuery } = filters
  const normalizedQuery = searchQuery.trim().toLocaleLowerCase()
  const visibleProjects = projects.filter((project) => {
    const matchesScope =
      scope === 'all' || followingAuthorIds.has(project.author.id)
    const matchesSearch =
      !normalizedQuery ||
      project.name.toLocaleLowerCase().includes(normalizedQuery) ||
      project.author.name.toLocaleLowerCase().includes(normalizedQuery)

    return matchesScope && matchesSearch
  })

  if (visibleProjects.length === 0) {
    return <p className={styles.emptyState}>No projects found.</p>
  }

  return (
    <div className={styles.grid}>
      {visibleProjects.map((project) => (
        <FeedProjectCard key={project.id} project={project} />
      ))}
    </div>
  )
}
