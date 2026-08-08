import { Header, LoginForm, RegisterForm } from '@widgets'
import styles from './MainPage.module.css'

export function MainPage() {
  return (
    <div className={styles.page}>
      <Header />
      <main className={styles.content}>
        <header className={styles.header}>
          <div>
          <p className={styles.eyebrow}>Shared UI · Auth widgets</p>
          <h1>Авторизация</h1>
          </div>
        </header>
        <div className={styles.showcase}>
          <RegisterForm />
          <LoginForm />
        </div>
      </main>
    </div>
  )
}
