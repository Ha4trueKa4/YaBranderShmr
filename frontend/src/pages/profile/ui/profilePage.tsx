import { authStore } from '@app/providers/auth'
import { AppRoute } from '@app/providers/router/routes'
import { Button } from '@shared/ui'
import { Header } from '@widgets/header'
import { useNavigate } from 'react-router-dom'

import styles from './profilePage.module.css'

export function ProfilePage() {
  const navigate = useNavigate()

  function handleLogout() {
    authStore.signOut()
    navigate(AppRoute.Auth, { replace: true })
  }

  return (
    <div className={styles.page}>
      <Header />
      <main className={styles.content}>
        <h1>Profile</h1>
        <Button onClick={handleLogout} variant="secondary">
          Log out
        </Button>
      </main>
    </div>
  )
}
