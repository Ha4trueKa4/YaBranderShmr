import { useState, type FormEvent } from 'react'
import { Button, FormField, IconButton, Input } from '@shared'
import brandMark from '@shared/assets/brandmark.svg'
import styles from './AuthForms.module.css'

export function LoginForm() {
  const [isPasswordVisible, setPasswordVisible] = useState(false)
  const [isSubmitted, setSubmitted] = useState(false)

  function handleSubmit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); setSubmitted(true) }

  return <section aria-labelledby="login-title" className={styles.card}>
    <img alt="Brandkit AI" className={styles.brand} src={brandMark} />
    <h2 className={styles.title} id="login-title">Войти</h2>
    <form className={styles.form} onSubmit={handleSubmit}>
      <div className={styles.formFields}>
        <FormField label="Email"><Input autoComplete="email" name="email" placeholder="hello@brandkit.ai" required type="email" /></FormField>
        <FormField label="Пароль"><Input autoComplete="current-password" name="password" placeholder="Введите пароль" required trailingContent={<IconButton aria-label={isPasswordVisible ? 'Скрыть пароль' : 'Показать пароль'} onClick={() => setPasswordVisible((visible) => !visible)} size="sm">{isPasswordVisible ? '◉' : '◌'}</IconButton>} type={isPasswordVisible ? 'text' : 'password'} /></FormField>
      </div>
      <Button fullWidth type="submit">Войти</Button>
      <button className={styles.link} type="button">Забыли пароль?</button>
      {isSubmitted && <p className={styles.success}>Форма готова к подключению к API.</p>}
    </form>
    <p className={styles.footer}>Нет аккаунта? <button className={styles.link} type="button">Зарегистрироваться</button></p>
  </section>
}
