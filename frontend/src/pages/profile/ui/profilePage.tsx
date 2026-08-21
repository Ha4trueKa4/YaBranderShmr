import { authStore } from '@app/providers/auth'
import { AppRoute } from '@app/providers/router/routes'
import {
  useDeleteProjectMutation,
  userProfileQueryKey,
  useUpdateProjectMetadataMutation,
  useUpdateUserProfileMutation,
  useUserProfileQuery,
} from '@entities/project'
import { getApiErrorMessage } from '@shared/api/api'
import { AsyncState, Button, ErrorState, useToast } from '@shared/ui'
import { useQueryClient } from '@tanstack/react-query'
import {
  Footer,
  OwnProjectsSection,
  ProfileHeader,
  type ProfileHeaderProfile,
} from '@widgets'
import { Header } from '@widgets/header'
import { Pencil } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { generatePath, useNavigate } from 'react-router-dom'

import { EditProfileModal } from './EditProfileModal'
import styles from './profilePage.module.css'

export function ProfilePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const toast = useToast()
  const { t } = useTranslation()
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
  const {
    isPending: isUpdatingProfilePending,
    mutateAsync: updateUserProfile,
  } = useUpdateUserProfileMutation()

  if (!session) {
    return <ProfileUnavailable />
  }

  const currentSession = session
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

  function handleOpenFollowing() {
    navigate(generatePath(AppRoute.UserSubscriptions, { userId }), {
      viewTransition: true,
    })
  }

  function handleOpenFollowers() {
    navigate(generatePath(AppRoute.UserSubscribers, { userId }), {
      viewTransition: true,
    })
  }

  function handleOpenProjects() {
    document.getElementById('projects')?.scrollIntoView({ behavior: 'smooth' })
  }

  async function handleEditProfile(name: string) {
    try {
      const updatedProfile = await updateUserProfile({
        nickname: name,
        userId,
      })
      const nickname = updatedProfile.nickname

      setProfileName(nickname)
      authStore.signIn({
        ...currentSession,
        user: { ...currentSession.user, nickname },
      })
      setEditProfileModalOpen(false)
      await queryClient.invalidateQueries({
        queryKey: userProfileQueryKey(userId),
      })
      toast.info(t('Profile updated.'))
    } catch (error) {
      toast.error(getApiErrorMessage(error))
      throw error
    }
  }

  async function handleDeleteProject(projectId: string) {
    try {
      await deleteProject(projectId)
      await queryClient.invalidateQueries({
        queryKey: userProfileQueryKey(userId),
      })
      toast.info(t('Project deleted.'))
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
      toast.info(t('Project updated.'))
    } catch (error) {
      toast.error(getApiErrorMessage(error))
      throw error
    }
  }

  async function handleProjectVersionRestored() {
    await queryClient.invalidateQueries({
      queryKey: userProfileQueryKey(userId),
    })
  }

  return (
    <div className={styles.page}>
      <Header />
      <main className={styles.content}>
        <ProfileHeader
          actions={
            <>
              <Button onClick={handleLogout} variant="ghost">
                {t('Log out')}
              </Button>
              <Button
                onClick={() => setEditProfileModalOpen(true)}
                variant="outline"
              >
                <Pencil aria-hidden size={16} />
                {t('Edit profile')}
              </Button>
            </>
          }
          profile={profile}
          onFollowersClick={handleOpenFollowers}
          onFollowingClick={handleOpenFollowing}
          onProjectsClick={handleOpenProjects}
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
            onProjectVersionRestored={handleProjectVersionRestored}
            onUpdateProjectMetadata={handleUpdateProjectMetadata}
            projects={projects}
          />
        </AsyncState>
      </main>
      <EditProfileModal
        isSubmitting={isUpdatingProfilePending}
        isOpen={isEditProfileModalOpen}
        name={profileName}
        onClose={() => setEditProfileModalOpen(false)}
        onSubmit={handleEditProfile}
      />
      <Footer />
    </div>
  )
}

function ProfileUnavailable() {
  const { t } = useTranslation()

  return (
    <div className={styles.page}>
      <Header />
      <main className={styles.content}>
        <ErrorState
          message={t(
            'Unable to load the profile. Please sign in and try again.',
          )}
        />
      </main>
      <Footer />
    </div>
  )
}
