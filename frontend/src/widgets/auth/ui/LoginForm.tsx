import { Button, FormField, IconButton, Input } from '@shared'
import { Eye, EyeOff } from 'lucide-react'
import { useId } from 'react'
import { Controller } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { type LoginSubmitHandler, useLoginForm } from '../model'
import yandexIcon from './assets/yandexIcon.svg'
import styles from './AuthForms.module.css'

interface LoginFormProps {
  isPending?: boolean
  onYandexSignIn?: () => void
  onSubmit?: LoginSubmitHandler
  onSwitchToRegister?: () => void
}

export function LoginForm({
  isPending = false,
  onYandexSignIn,
  onSubmit,
  onSwitchToRegister,
}: LoginFormProps) {
  const { t } = useTranslation()
  const emailInputId = useId()
  const passwordInputId = useId()
  const { form, isPasswordVisible, submit, togglePasswordVisibility } =
    useLoginForm({ onSubmit })

  return (
    <section className={styles.formSection}>
      <header className={styles.header}>
        <h1 className={styles.title}>{t('Welcome back')}</h1>
        <p className={styles.subtitle}>{t('Sign in to continue creating.')}</p>
      </header>
      <form className={styles.form} onSubmit={submit}>
        <div className={styles.formFields}>
          <Controller
            control={form.control}
            name="email"
            render={({ field, fieldState }) => (
              <FormField
                error={fieldState.error?.message}
                htmlFor={emailInputId}
                label={t('Email')}
                reserveErrorSpace
              >
                <Input
                  {...field}
                  autoComplete="email"
                  className={styles.inputControl}
                  invalid={Boolean(fieldState.error)}
                  id={emailInputId}
                  placeholder="alex@studio.com"
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
              <FormField
                error={fieldState.error?.message}
                htmlFor={passwordInputId}
                label={t('Password')}
                reserveErrorSpace
              >
                <Input
                  {...field}
                  autoComplete="current-password"
                  className={styles.inputControl}
                  invalid={Boolean(fieldState.error)}
                  id={passwordInputId}
                  placeholder={t('Enter your password')}
                  ref={field.ref}
                  trailingContent={
                    <IconButton onClick={togglePasswordVisibility} size="sm">
                      {isPasswordVisible ? (
                        <EyeOff size={18} strokeWidth={1.75} />
                      ) : (
                        <Eye size={18} strokeWidth={1.75} />
                      )}
                    </IconButton>
                  }
                  type={isPasswordVisible ? 'text' : 'password'}
                />
              </FormField>
            )}
          />
        </div>
        <Button fullWidth loading={isPending} size="lg" type="submit">
          {t('Sign in')}
        </Button>
      </form>
      {onYandexSignIn && (
        <Button
          className={styles.yandexButton}
          fullWidth
          onClick={onYandexSignIn}
          size="lg"
          variant="outline"
        >
          <img alt="" className={styles.yandexIcon} src={yandexIcon} />
          {t('Continue with Yandex ID')}
        </Button>
      )}
      <p className={styles.footer}>
        {t('New to Brandkit AI?')}{' '}
        <button
          className={styles.link}
          onClick={onSwitchToRegister}
          type="button"
        >
          {t('Create account')}
        </button>
      </p>
    </section>
  )
}
