import { authStore } from '@app/providers/auth'
import { AppRoute } from '@app/providers/router/routes'
import { useProjectsQuery } from '@entities/project'
import { Button } from '@shared/ui'
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
  const { data: projects = [] } = useProjectsQuery()
  // TODO: Replace temporary profile details when the API returns them.
  const profile: ProfileHeaderProfile = {
    bio: 'Building ideas into brands. Product designer and creative technologist.',
    followersCount: 340,
    followingCount: 28,
    name: 'Oliver Lee',
    projectsCount: 12,
    username: 'oliverlee',
  }

  function handleLogout() {
    authStore.signOut()
    navigate(AppRoute.Auth, { replace: true })
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
        <OwnProjectsSection projects={projects} />
      </main>
    </div>
  )
}
