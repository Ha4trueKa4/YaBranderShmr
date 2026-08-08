import { Controller } from 'react-hook-form'
import { Button, FormField, IconButton, Input } from '@shared'
import brandMark from '@shared/assets/brandmark.svg'
import { useRegisterForm, type RegisterSubmitHandler } from '../model'
import styles from './AuthForms.module.css'

interface RegisterFormProps {
  onSubmit?: RegisterSubmitHandler
  onSwitchToLogin?: () => void
}

export function RegisterForm({ onSubmit, onSwitchToLogin }: RegisterFormProps) {
  const {
    form,
    isPasswordVisible,
    isSubmitted,
    submit,
    togglePasswordVisibility,
  } = useRegisterForm({ onSubmit })

  return (
    <section className={styles.card}>
      <img alt="Brandkit AI" className={styles.brand} src={brandMark} />
      <h2 className={styles.title}>Создать аккаунт</h2>
      <form className={styles.form} onSubmit={submit}>
        <div className={styles.formFields}>
          <Controller
            control={form.control}
            name="nickname"
            render={({ field, fieldState }) => (
              <FormField error={fieldState.error?.message} label="Никнейм">
                <Input
                  {...field}
                  autoComplete="name"
                  invalid={Boolean(fieldState.error)}
                  placeholder="Иван Иванов"
                  ref={field.ref}
                />
              </FormField>
            )}
          />
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
                  autoComplete="new-password"
                  invalid={Boolean(fieldState.error)}
                  placeholder="Придумайте пароль"
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
          Создать аккаунт
        </Button>
        {isSubmitted && (
          <p className={styles.success}>Форма успешно отправлена.</p>
        )}
      </form>
      <p className={styles.footer}>
        Уже есть аккаунт?{' '}
        <button className={styles.link} onClick={onSwitchToLogin} type="button">
          Войти
        </button>
      </p>
    </section>
  )
}
