import { OwnProjectCard } from '@entities/project'
import type { Project } from '@shared/api/types'
import { ConfirmationModal } from '@shared/ui'
import { useEffect, useState } from 'react'

import styles from './OwnProjectsSection.module.css'
import { ProjectManagementModal } from './ProjectManagementModal'

interface OwnProjectsSectionProps {
  onOpenProject: (projectId: string) => void
  projects: Project[]
}

export function OwnProjectsSection({
  onOpenProject,
  projects,
}: OwnProjectsSectionProps) {
  const [managedProjects, setManagedProjects] = useState(projects)
  const [editedProjectId, setEditedProjectId] = useState<string | null>(null)
  const [deletedProjectId, setDeletedProjectId] = useState<string | null>(null)

  useEffect(() => {
    setManagedProjects(projects)
  }, [projects])

  const editedProject = managedProjects.find(
    (project) => project.id === editedProjectId,
  )
  const deletedProject = managedProjects.find(
    (project) => project.id === deletedProjectId,
  )

  function updateEditedProject(update: Partial<Project>) {
    if (!editedProjectId) {
      return
    }

    setManagedProjects((currentProjects) =>
      currentProjects.map((project) =>
        project.id === editedProjectId ? { ...project, ...update } : project,
      ),
    )
  }

  function handleDeleteProject() {
    if (!deletedProjectId) {
      return
    }

    setManagedProjects((currentProjects) =>
      currentProjects.filter((project) => project.id !== deletedProjectId),
    )
    setDeletedProjectId(null)
  }

  return (
    <section className={styles.projectsSection}>
      <div className={styles.sectionHeader}>
        <div>
          <h2>All projects</h2>
          <p>Manage visibility and versions</p>
        </div>
        <span>{managedProjects.length} projects</span>
      </div>
      <div className={styles.projectsGrid}>
        {managedProjects.map((project) => (
          <OwnProjectCard
            key={project.id}
            onDeleteClick={() => {
              if (project.id) {
                setDeletedProjectId(project.id)
              }
            }}
            onEditClick={() => {
              if (project.id) {
                setEditedProjectId(project.id)
              }
            }}
            onOpenClick={() => {
              if (project.id) {
                onOpenProject(project.id)
              }
            }}
            project={project}
          />
        ))}
      </div>
      {editedProject?.id && (
        <ProjectManagementModal
          onClose={() => setEditedProjectId(null)}
          onSubmit={({ isPublic, name }) => {
            updateEditedProject({ is_public: isPublic, name })
            setEditedProjectId(null)
          }}
          project={editedProject}
        />
      )}
      <ConfirmationModal
        confirmLabel="Delete project"
        description={`Delete “${deletedProject?.name ?? 'Untitled project'}”? This action cannot be undone.`}
        isOpen={Boolean(deletedProject)}
        onCancel={() => setDeletedProjectId(null)}
        onConfirm={handleDeleteProject}
        title="Delete project?"
      />
    </section>
  )
}
