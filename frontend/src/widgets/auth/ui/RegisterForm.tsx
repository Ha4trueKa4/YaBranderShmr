import { useState, type FormEvent } from 'react'
import { Button, FormField, IconButton, Input } from '@shared'
import brandMark from '@shared/assets/brandmark.svg'
import styles from './AuthForms.module.css'

export function RegisterForm() {
  const [isPasswordVisible, setPasswordVisible] = useState(false)
  const [isSubmitted, setSubmitted] = useState(false)

  function handleSubmit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); setSubmitted(true) }

  return <section aria-labelledby="register-title" className={styles.card}>
    <img alt="Brandkit AI" className={styles.brand} src={brandMark} />
    <h2 className={styles.title} id="register-title">Создать аккаунт</h2>
    <form className={styles.form} onSubmit={handleSubmit}>
      <div className={styles.formFields}>
        <FormField label="Полное имя"><Input autoComplete="name" name="name" placeholder="Иван Иванов" required /></FormField>
        <FormField label="Email"><Input autoComplete="email" name="email" placeholder="hello@brandkit.ai" required type="email" /></FormField>
        <FormField label="Пароль"><Input autoComplete="new-password" name="password" placeholder="Придумайте пароль" required trailingContent={<IconButton aria-label={isPasswordVisible ? 'Скрыть пароль' : 'Показать пароль'} onClick={() => setPasswordVisible((visible) => !visible)} size="sm">{isPasswordVisible ? '◉' : '◌'}</IconButton>} type={isPasswordVisible ? 'text' : 'password'} /></FormField>
      </div>
      <Button fullWidth type="submit">Создать аккаунт</Button>
      {isSubmitted && <p className={styles.success}>Аккаунт будет создан после подключения API.</p>}
    </form>
    <p className={styles.footer}>Уже есть аккаунт? <button className={styles.link} type="button">Войти</button></p>
  </section>
}
