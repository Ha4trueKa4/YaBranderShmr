import { authStore } from '@app/providers/auth'
import { AppRoute } from '@app/providers/router/routes'
import { useUserProfileQuery } from '@entities/project'
import { getApiErrorMessage } from '@shared/api/api'
import { AsyncState, Button, ErrorState } from '@shared/ui'
import {
  OwnProjectsSection,
  ProfileHeader,
  type ProfileHeaderProfile,
} from '@widgets'
import { Header } from '@widgets/header'
import { useNavigate } from 'react-router-dom'

import styles from './profilePage.module.css'

export function ProfilePage() {
  const navigate = useNavigate()
  const session = authStore.getSession
  const userId = session?.user.id ?? ''
  const {
    data: userProfile,
    error: userProfileError,
    isError: isUserProfileError,
    isPending: isUserProfilePending,
  } = useUserProfileQuery(userId)

  if (!session) {
    return <ProfileUnavailable />
  }

  const projects = userProfile?.projects ?? []

  const profile: ProfileHeaderProfile = {
    followersCount: userProfile?.subscribers_count ?? 0,
    followingCount: userProfile?.subscriptions_count ?? 0,
    name: session.user.nickname,
    projectsCount: projects.length,
    username: session.user.nickname,
  }

  function handleLogout() {
    authStore.signOut()
    navigate(AppRoute.Auth, { replace: true, viewTransition: true })
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
              <Button variant="secondary">Edit profile</Button>
            </>
          }
          profile={profile}
        />
        <AsyncState
          errorMessage={getApiErrorMessage(userProfileError)}
          isError={isUserProfileError}
          isPending={isUserProfilePending}
        >
          <OwnProjectsSection projects={projects} />
        </AsyncState>
      </main>
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
