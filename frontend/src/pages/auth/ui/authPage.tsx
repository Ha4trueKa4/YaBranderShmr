import { authStore } from '@app/providers/auth'
import { LoginForm, RegisterForm } from '@widgets'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import heroCanvas from '../assets/heroCanvas.svg'
import heroLogoShape from '../assets/heroLogoShape.svg'
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
              onSubmit={handleMockAuthorization}
              onSwitchToRegister={() => setActiveForm('register')}
            />
          ) : (
            <RegisterForm
              onSubmit={handleMockAuthorization}
              onSwitchToLogin={() => setActiveForm('login')}
            />
          )}
        </div>
      </section>
    </main>
  )
}
