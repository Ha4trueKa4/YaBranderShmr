import { useSearchParams } from 'react-router-dom'
import { LoginForm, RegisterForm } from '@widgets'

import styles from './authPage.module.css'

export function AuthPage() {
  const [searchParams] = useSearchParams()
  const isRegisterMode = searchParams.get('mode') === 'register'

  return (
    <main className={styles.page}>
      {isRegisterMode ? <RegisterForm /> : <LoginForm />}
    </main>
  )
}
