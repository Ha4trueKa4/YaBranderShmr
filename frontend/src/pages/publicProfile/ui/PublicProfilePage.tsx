import { authStore } from '@app/providers/auth'
import {
  type PublicProfileProject,
  userProfileQueryKey,
  useSubscribeToUserMutation,
  useUnsubscribeFromUserMutation,
  useUserProfileQuery,
} from '@entities/project'
import { getApiErrorMessage } from '@shared/api/api'
import { AsyncState, Button, useToast } from '@shared/ui'
import { useQueryClient } from '@tanstack/react-query'
import {
  ProfileHeader,
  type ProfileHeaderProfile,
  PublicProjectsSection,
} from '@widgets'
import { Header } from '@widgets/header'
import { observer } from 'mobx-react-lite'
import { useNavigate, useParams } from 'react-router-dom'

import styles from './PublicProfilePage.module.css'

export const PublicProfilePage = observer(function PublicProfilePage() {
  const { userId = '' } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const toast = useToast()
  const isAuthorized = authStore.isAuthorized
  const isOwnProfile = authStore.getSession?.user.id === userId
  const {
    data: userProfile,
    error: userProfileError,
    isError: isUserProfileError,
    isPending: isUserProfilePending,
  } = useUserProfileQuery(userId)
  const { isPending: isSubscribingPending, mutateAsync: subscribeToUser } =
    useSubscribeToUserMutation()
  const {
    isPending: isUnsubscribingPending,
    mutateAsync: unsubscribeFromUser,
  } = useUnsubscribeFromUserMutation()

  const projects = toPublicProfileProjects(userProfile?.projects ?? [])
  const profile: ProfileHeaderProfile = {
    followersCount: userProfile?.subscribers_count ?? 0,
    followingCount: userProfile?.subscriptions_count ?? 0,
    name: userProfile?.name ?? '',
    projectsCount: projects.length,
    username: userProfile?.name ?? '',
  }
  const isSubscribed = userProfile?.is_subscribed ?? false
  const isFollowPending = isSubscribingPending || isUnsubscribingPending

  async function handleFollowClick() {
    if (!isAuthorized) {
      navigate('/auth', { viewTransition: true })

      return
    }

    try {
      if (isSubscribed) {
        await unsubscribeFromUser(userId)
      } else {
        await subscribeToUser(userId)
      }

      await queryClient.invalidateQueries({
        queryKey: userProfileQueryKey(userId),
      })
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    }
  }

  return (
    <div className={styles.page}>
      <Header />
      <main className={styles.content}>
        <AsyncState
          errorMessage={getApiErrorMessage(userProfileError)}
          isError={isUserProfileError}
          isPending={isUserProfilePending}
        >
          <ProfileHeader
            actions={
              !isOwnProfile && (
                <Button
                  loading={isFollowPending}
                  minWidth="7rem"
                  onClick={handleFollowClick}
                  variant={isSubscribed ? 'ghost' : 'primary'}
                >
                  {isSubscribed ? 'Unfollow' : 'Follow'}
                </Button>
              )
            }
            profile={profile}
          />
          <PublicProjectsSection projects={projects} />
        </AsyncState>
      </main>
    </div>
  )
})

function toPublicProfileProjects(
  projects: NonNullable<
    ReturnType<typeof useUserProfileQuery>['data']
  >['projects'],
): PublicProfileProject[] {
  return (projects ?? []).flatMap((project) => {
    if (!project.id || !project.name) {
      return []
    }

    return [
      {
        description: project.description ?? '',
        id: project.id,
        name: project.name,
        previewUrl: project.url ?? null,
      },
    ]
  })
}
