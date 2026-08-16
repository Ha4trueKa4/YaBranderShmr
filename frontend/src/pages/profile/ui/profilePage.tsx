import { authStore } from '@app/providers/auth'
import { AppRoute } from '@app/providers/router/routes'
import {
  useDeleteProjectMutation,
  userProfileQueryKey,
  useUpdateProjectMetadataMutation,
  useUserProfileQuery,
} from '@entities/project'
import { getApiErrorMessage } from '@shared/api/api'
import { AsyncState, Button, ErrorState, useToast } from '@shared/ui'
import { useQueryClient } from '@tanstack/react-query'
import {
  OwnProjectsSection,
  ProfileHeader,
  type ProfileHeaderProfile,
} from '@widgets'
import { Header } from '@widgets/header'
import { useState } from 'react'
import { generatePath, useNavigate } from 'react-router-dom'

import { EditProfileModal } from './EditProfileModal'
import styles from './profilePage.module.css'

export function ProfilePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const toast = useToast()
  const session = authStore.getSession
  const [isEditProfileModalOpen, setEditProfileModalOpen] = useState(false)
  const [profileName, setProfileName] = useState(
    () => session?.user.nickname ?? '',
  )
  const userId = session?.user.id ?? ''
  const {
    data: userProfile,
    error: userProfileError,
    isError: isUserProfileError,
    isPending: isUserProfilePending,
  } = useUserProfileQuery(userId)
  const { isPending: isDeletingProjectPending, mutateAsync: deleteProject } =
    useDeleteProjectMutation()
  const { mutateAsync: updateProjectMetadata } =
    useUpdateProjectMetadataMutation()

  if (!session) {
    return <ProfileUnavailable />
  }

  const projects = userProfile?.projects ?? []

  const profile: ProfileHeaderProfile = {
    followersCount: userProfile?.subscribers_count ?? 0,
    followingCount: userProfile?.subscriptions_count ?? 0,
    name: profileName,
    projectsCount: projects.length,
    username: profileName,
  }

  function handleLogout() {
    authStore.signOut()
    navigate(AppRoute.Auth, { replace: true, viewTransition: true })
  }

  function handleOpenProject(projectId: string) {
    navigate(generatePath(AppRoute.EditorProject, { projectId }), {
      viewTransition: true,
    })
  }

  function handleEditProfile(name: string) {
    setProfileName(name)
    setEditProfileModalOpen(false)
  }

  async function handleDeleteProject(projectId: string) {
    try {
      await deleteProject(projectId)
      await queryClient.invalidateQueries({
        queryKey: userProfileQueryKey(userId),
      })
      toast.info('Project deleted.')
    } catch (error) {
      toast.error(getApiErrorMessage(error))
      throw error
    }
  }

  async function handleUpdateProjectMetadata(
    projectId: string,
    { isPublic, name }: { isPublic: boolean; name: string },
  ) {
    try {
      await updateProjectMetadata({
        is_public: isPublic,
        name,
        projectId,
      })
      await queryClient.invalidateQueries({
        queryKey: userProfileQueryKey(userId),
      })
      toast.info('Project updated.')
    } catch (error) {
      toast.error(getApiErrorMessage(error))
      throw error
    }
  }

  return (
    <div className={styles.page}>
      <Header />
      <main className={styles.content}>
        <ProfileHeader
          actions={
            <>
              <Button onClick={handleLogout} variant="ghost">
                Log out
              </Button>
              <Button
                onClick={() => setEditProfileModalOpen(true)}
                variant="secondary"
              >
                Edit profile
              </Button>
            </>
          }
          profile={profile}
        />
        <AsyncState
          errorMessage={getApiErrorMessage(userProfileError)}
          isError={isUserProfileError}
          isPending={isUserProfilePending}
        >
          <OwnProjectsSection
            isDeletingProjectPending={isDeletingProjectPending}
            onDeleteProject={handleDeleteProject}
            onOpenProject={handleOpenProject}
            onUpdateProjectMetadata={handleUpdateProjectMetadata}
            projects={projects}
          />
        </AsyncState>
      </main>
      <EditProfileModal
        isOpen={isEditProfileModalOpen}
        name={profileName}
        onClose={() => setEditProfileModalOpen(false)}
        onSubmit={handleEditProfile}
      />
    </div>
  )
}

function ProfileUnavailable() {
  return (
    <div className={styles.page}>
      <Header />
      <main className={styles.content}>
        <ErrorState message="Unable to load the profile. Please sign in and try again." />
      </main>
    </div>
  )
}
