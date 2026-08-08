import { LoginForm, RegisterForm } from '@widgets'
import styles from './MainPage.module.css'

export function MainPage() {
  return (
    <main className={styles.page}>
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
  )
}
