import { useEffect, useState } from 'react'
import { Button } from '@shared'
import { LoginForm, RegisterForm } from '@widgets'
import styles from './MainPage.module.css'

export function MainPage() {
  const [theme, setTheme] = useState<'dark' | 'light'>('dark')
  useEffect(() => { document.documentElement.dataset.theme = theme }, [theme])

  return <main className={styles.page}>
    <header className={styles.header}>
      <div><p className={styles.eyebrow}>Shared UI · Auth widgets</p><h1>Авторизация</h1></div>
      <Button onClick={() => setTheme((currentTheme) => currentTheme === 'dark' ? 'light' : 'dark')} size="sm" variant="secondary">{theme === 'dark' ? 'Светлая тема' : 'Тёмная тема'}</Button>
    </header>
    <div className={styles.showcase}><RegisterForm /><LoginForm /></div>
  </main>
}
