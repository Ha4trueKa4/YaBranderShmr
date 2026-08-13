import { authStore } from '@app/providers/auth'
import { AppRoute } from '@app/providers/router/routes'
import type { UserProfile } from '@shared/api/types'
import { Button } from '@shared/ui'
import { ProfileHeader } from '@widgets'
import { Header } from '@widgets/header'
import { useNavigate } from 'react-router-dom'

import styles from './profilePage.module.css'

export function ProfilePage() {
  const navigate = useNavigate()
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
        <ProfileHeader user={user} />
        <Button onClick={handleLogout} variant="danger">
          Log out
        </Button>
      </main>
    </div>
  )
}
