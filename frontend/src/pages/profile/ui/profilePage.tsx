import { authStore } from '@app/providers/auth'
import { AppRoute } from '@app/providers/router/routes'
import { useProjectsQuery } from '@entities/project'
import type { UserProfile } from '@shared/api/types'
import { OwnProjectsSection, ProfileHeader } from '@widgets'
import { Header } from '@widgets/header'
import { useNavigate } from 'react-router-dom'

import styles from './profilePage.module.css'

export function ProfilePage() {
  const navigate = useNavigate()
  const { data: projects = [] } = useProjectsQuery()
  const user: UserProfile = {
    id: authStore.session?.userId,
    name: 'Oliver Lee',
  }

  function handleLogout() {
    authStore.signOut()
    navigate(AppRoute.Auth, { replace: true })
  }

  return (
    <div className={styles.page}>
      <Header />
      <main className={styles.content}>
        <ProfileHeader onLogoutClick={handleLogout} user={user} />
        <OwnProjectsSection projects={projects} />
      </main>
    </div>
  )
}
