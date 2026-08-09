import { authStore } from '@app/providers/auth'
import { LoginForm, RegisterForm } from '@widgets'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

import styles from './authPage.module.css'

type AuthForm = 'login' | 'register'

export function AuthPage() {
  const navigate = useNavigate()
  const [activeForm, setActiveForm] = useState<AuthForm>('login')

  function handleMockAuthorization() {
    authStore.signIn({
      accessToken: 'mock-access-token',
      userId: 'mock-user-id',
    })
    navigate('/', { replace: true })
  }

  return (
    <main className={styles.page}>
      {activeForm === 'login' ? (
        <LoginForm
          onSubmit={handleMockAuthorization}
          onSwitchToRegister={() => setActiveForm('register')}
        />
      ) : (
        <RegisterForm
          onSubmit={handleMockAuthorization}
          onSwitchToLogin={() => setActiveForm('login')}
        />
      )}
    </main>
  )
}
