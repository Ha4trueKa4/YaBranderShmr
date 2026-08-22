import { type PublicProfileProject, PublicProjectCard } from '@entities/project'
import { useTranslation } from 'react-i18next'

import styles from './PublicProjectsSection.module.css'

interface PublicProjectsSectionProps {
  projects: PublicProfileProject[]
}

export function PublicProjectsSection({
  projects,
}: PublicProjectsSectionProps) {
  const { t } = useTranslation()
  return (
    <section className={styles.projectsSection}>
      <div className={styles.sectionHeader}>
        <div>
          <h2>{t('Public projects')}</h2>
          <p>{t('Only publicly shared work is visible')}</p>
        </div>
        <span>
          {t('{{count}} public projects', { count: projects.length })}
        </span>
      </div>
      <div className={styles.projectsGrid}>
        {projects.map((project) => (
          <PublicProjectCard key={project.id} project={project} />
        ))}
      </div>
    </section>
  )
}
