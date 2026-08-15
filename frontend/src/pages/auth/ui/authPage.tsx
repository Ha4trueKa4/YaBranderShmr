import { authStore } from '@app/providers/auth'
import {
  getAuthErrorMessage,
  useLoginMutation,
  useRegisterMutation,
} from '@features/auth'
import {type AuthResponse, type LoginRequest, type RegisterRequest } from '@shared/api/types'
import { useToast } from '@shared/ui'
import { LoginForm, RegisterForm } from '@widgets'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import heroCanvas from '../assets/heroCanvas.svg'
import heroLogoShape from '../assets/heroLogoShape.svg'
import styles from './authPage.module.css'

type AuthForm = 'login' | 'register'

export function AuthPage() {
  const navigate = useNavigate()
  const toast = useToast()
  const [activeForm, setActiveForm] = useState<AuthForm>('login')
  const { isPending: isLoggingInPending, mutateAsync: login } =
    useLoginMutation()
  const { isPending: isRegisteringPending, mutateAsync: register } =
    useRegisterMutation()

  function signIn({access_token, user}: AuthResponse) {
    authStore.signIn({
      accessToken: access_token,
      user,
    })
    navigate('/', { replace: true, viewTransition: true })
  }

  function handleAuthError(error: unknown) {
    const authError = error instanceof Error ? error : new Error()

    toast.error(getAuthErrorMessage(authError))
  }

  async function handleLogin(values: LoginRequest) {
    try {
      const response = await login(values)
      toast.info('You are signed in.')
      signIn(response)
    } catch (error) {
      handleAuthError(error)
    }
  }

  async function handleRegister(values: RegisterRequest) {
    try {
      const response = await register(values)
      toast.info('Your account has been created.')
      signIn(response)
    } catch (error) {
      handleAuthError(error)
    }
  }

  return (
    <main className={styles.page}>
      <aside className={styles.promoPanel}>
        <p className={styles.brandName}>Brandkit AI</p>
        <div className={styles.promoContent}>
          <h1 className={styles.promoTitle}>
            Your next brand
            <br />
            starts here.
          </h1>
          <p className={styles.promoDescription}>
            Turn a prompt into a complete visual identity, then refine it in the
            editor.
          </p>
        </div>
        <div className={styles.brandPreview}>
          <img alt="" className={styles.previewCanvas} src={heroCanvas} />
          <img alt="" className={styles.previewMark} src={heroLogoShape} />
        </div>
      </aside>
      <section className={styles.formPanel}>
        <div className={styles.formContent}>
          {activeForm === 'login' ? (
            <LoginForm
              isPending={isLoggingInPending}
              onSubmit={handleLogin}
              onSwitchToRegister={() => setActiveForm('register')}
            />
          ) : (
            <RegisterForm
              isPending={isRegisteringPending}
              onSubmit={handleRegister}
              onSwitchToLogin={() => setActiveForm('login')}
            />
          )}
        </div>
      </section>
    </main>
  )
}
