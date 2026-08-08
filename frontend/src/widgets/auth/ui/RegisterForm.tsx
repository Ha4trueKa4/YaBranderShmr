import { Controller } from 'react-hook-form'
import { Button, FormField, IconButton, Input } from '@shared'
import brandMark from '@shared/assets/brandmark.svg'
import { useRegisterForm, type RegisterSubmitHandler } from '../model'
import styles from './AuthForms.module.css'

interface RegisterFormProps {
  onSubmit?: RegisterSubmitHandler
}

export function RegisterForm({ onSubmit }: RegisterFormProps) {
  const {
    form,
    isPasswordVisible,
    isSubmitted,
    submit,
    togglePasswordVisibility,
  } = useRegisterForm({ onSubmit })

  return (
    <section aria-labelledby="register-title" className={styles.card}>
      <img alt="Brandkit AI" className={styles.brand} src={brandMark} />
      <h2 className={styles.title} id="register-title">
        Создать аккаунт
      </h2>
      <form className={styles.form} onSubmit={submit}>
        <div className={styles.formFields}>
          <Controller
            control={form.control}
            name="fullName"
            render={({ field, fieldState }) => (
              <FormField error={fieldState.error?.message} label="Полное имя">
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
                    <IconButton
                      aria-label={
                        isPasswordVisible ? 'Скрыть пароль' : 'Показать пароль'
                      }
                      onClick={togglePasswordVisibility}
                      size="sm"
                    >
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
        <button className={styles.link} type="button">
          Войти
        </button>
      </p>
    </section>
  )
}
