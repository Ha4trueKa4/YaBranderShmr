import { Controller } from 'react-hook-form'
import { Button, FormField, IconButton, Input } from '@shared'
import brandMark from '@shared/assets/brandmark.svg'
import { useLoginForm, type LoginSubmitHandler } from '../model'
import styles from './AuthForms.module.css'

interface LoginFormProps {
  onSubmit?: LoginSubmitHandler
  onSwitchToRegister?: () => void
}

export function LoginForm({ onSubmit, onSwitchToRegister }: LoginFormProps) {
  const {
    form,
    isPasswordVisible,
    isSubmitted,
    submit,
    togglePasswordVisibility,
  } = useLoginForm({ onSubmit })

  return (
    <section className={styles.card}>
      <img alt="Brandkit AI" className={styles.brand} src={brandMark} />
      <h2 className={styles.title}>Войти</h2>
      <form className={styles.form} onSubmit={submit}>
        <div className={styles.formFields}>
          <Controller
            control={form.control}
            name="email"
            render={({ field, fieldState }) => (
              <FormField error={fieldState.error?.message} label="Email">
                <Input
                  {...field}
                  autoComplete="email"
                  invalid={Boolean(fieldState.error)}
                  placeholder="hello@brandkit.ai"
                  ref={field.ref}
                  type="email"
                />
              </FormField>
            )}
          />
          <Controller
            control={form.control}
            name="password"
            render={({ field, fieldState }) => (
              <FormField error={fieldState.error?.message} label="Пароль">
                <Input
                  {...field}
                  autoComplete="current-password"
                  invalid={Boolean(fieldState.error)}
                  placeholder="Введите пароль"
                  ref={field.ref}
                  trailingContent={
                    <IconButton onClick={togglePasswordVisibility} size="sm">
                      {isPasswordVisible ? '◉' : '◌'}
                    </IconButton>
                  }
                  type={isPasswordVisible ? 'text' : 'password'}
                />
              </FormField>
            )}
          />
        </div>
        <Button fullWidth type="submit">
          Войти
        </Button>
        <button className={styles.link} type="button">
          Забыли пароль?
        </button>
        {isSubmitted && (
          <p className={styles.success}>Форма успешно отправлена.</p>
        )}
      </form>
      <p className={styles.footer}>
        Нет аккаунта?{' '}
        <button
          className={styles.link}
          onClick={onSwitchToRegister}
          type="button"
        >
          Зарегистрироваться
        </button>
      </p>
    </section>
  )
}
