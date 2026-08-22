import { OwnProjectCard } from '@entities/project'
import { generationProcessStore } from '@features/editorGenerate'
import { VersionHistoryModal } from '@features/editorVersionHistory'
import type { Project } from '@shared/api/types'
import { ConfirmationModal } from '@shared/ui'
import { observer } from 'mobx-react-lite'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'

import styles from './OwnProjectsSection.module.css'
import { ProjectManagementModal } from './ProjectManagementModal'

interface OwnProjectsSectionProps {
  isDeletingProjectPending: boolean
  onDeleteProject: (projectId: string) => Promise<void>
  onOpenProject: (projectId: string) => void
  onProjectVersionRestored: () => Promise<void>
  onUpdateProjectMetadata: (
    projectId: string,
    metadata: { isPublic: boolean; name: string },
  ) => Promise<void>
  projects: Project[]
}

export const OwnProjectsSection = observer(function OwnProjectsSection({
  isDeletingProjectPending,
  onDeleteProject,
  onOpenProject,
  onProjectVersionRestored,
  onUpdateProjectMetadata,
  projects,
}: OwnProjectsSectionProps) {
  const { t } = useTranslation()
  const [managedProjects, setManagedProjects] = useState(projects)
  const [editedProjectId, setEditedProjectId] = useState<string | null>(null)
  const [versionHistoryProjectId, setVersionHistoryProjectId] = useState<
    string | null
  >(null)
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

  async function handleDeleteProject() {
    const projectId = deletedProjectId

    if (!projectId) {
      return
    }

    try {
      await onDeleteProject(projectId)
    } catch {
      return
    }

    setManagedProjects((currentProjects) =>
      currentProjects.filter((project) => project.id !== projectId),
    )
    setDeletedProjectId(null)
  }

  return (
    <section className={styles.projectsSection} id="projects">
      <div className={styles.sectionHeader}>
        <div>
          <h2>{t('All projects')}</h2>
          <p>{t('Manage visibility and versions')}</p>
        </div>
        <span>
          {t('{{count}} projects', { count: managedProjects.length })}
        </span>
      </div>
      <div className={styles.projectsGrid}>
        {managedProjects.map((project) => (
          <OwnProjectCard
            isGenerating={
              generationProcessStore.isInProgress &&
              generationProcessStore.projectId === project.id
            }
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
          onOpenVersionHistory={() => {
            setEditedProjectId(null)
            setVersionHistoryProjectId(editedProject.id ?? null)
          }}
          onSubmit={async ({ isPublic, name }) => {
            const projectId = editedProject.id

            if (!projectId) {
              return
            }

            await onUpdateProjectMetadata(projectId, { isPublic, name })
            updateEditedProject({ is_public: isPublic, name })
            setEditedProjectId(null)
          }}
          project={editedProject}
        />
      )}
      {versionHistoryProjectId && (
        <VersionHistoryModal
          hasUnsavedChanges={false}
          isOpen
          onBack={() => {
            setVersionHistoryProjectId(null)
            setEditedProjectId(versionHistoryProjectId)
          }}
          onClose={() => setVersionHistoryProjectId(null)}
          onRestoreDocument={() => undefined}
          onVersionRestored={onProjectVersionRestored}
          projectId={versionHistoryProjectId}
        />
      )}
      <ConfirmationModal
        confirmLabel={t('Delete project')}
        description={t(
          'Delete “{{projectName}}”? This action cannot be undone.',
          {
            projectName: deletedProject?.name ?? t('Untitled project'),
          },
        )}
        isConfirming={isDeletingProjectPending}
        isOpen={Boolean(deletedProject)}
        onCancel={() => setDeletedProjectId(null)}
        onConfirm={handleDeleteProject}
        title={t('Delete project?')}
      />
    </section>
  )
})
