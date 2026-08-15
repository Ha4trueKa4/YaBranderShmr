import { type PublicProfileProject, PublicProjectCard } from '@entities/project'

import styles from './PublicProjectsSection.module.css'

interface PublicProjectsSectionProps {
  projects: PublicProfileProject[]
}

export function PublicProjectsSection({
  projects,
}: PublicProjectsSectionProps) {
  return (
    <section className={styles.projectsSection}>
      <div className={styles.sectionHeader}>
        <div>
          <h2>Public projects</h2>
          <p>Only publicly shared work is visible</p>
        </div>
        <span>{projects.length} public projects</span>
      </div>
      <div className={styles.projectsGrid}>
        {projects.map((project) => (
          <PublicProjectCard key={project.id} project={project} />
        ))}
      </div>
    </section>
  )
}
