import { OwnProjectCard } from '@entities/project'
import type { Project } from '@shared/api/types'

import styles from './OwnProjectsSection.module.css'

interface OwnProjectsSectionProps {
  projects: Project[]
}

export function OwnProjectsSection({ projects }: OwnProjectsSectionProps) {
  return (
    <section className={styles.projectsSection}>
      <div className={styles.sectionHeader}>
        <div>
          <h2>All projects</h2>
          <p>Manage visibility and versions</p>
        </div>
        <span>{projects.length} projects</span>
      </div>
      <div className={styles.projectsGrid}>
        {projects.map((project) =>
            <OwnProjectCard
              isPrivate={!project.is_public}
              key={project.id}
              title={project.name ?? 'Untitled project'}
              updatedAt={project.is_public ? 'published 2 days ago' : 'draft'}
            />
        )}
      </div>
    </section>
  )
}
