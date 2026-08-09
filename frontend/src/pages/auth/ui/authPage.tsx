import { LoginForm, RegisterForm } from '@widgets'
import { useState } from 'react'
import styles from './authPage.module.css'

type AuthForm = 'login' | 'register'

export function AuthPage() {
  const [activeForm, setActiveForm] = useState<AuthForm>('login')

  return (
    <main className={styles.page}>
      {activeForm === 'login' ? (
        <LoginForm onSwitchToRegister={() => setActiveForm('register')} />
      ) : (
        <RegisterForm onSwitchToLogin={() => setActiveForm('login')} />
      )}
    </main>
  )
}
