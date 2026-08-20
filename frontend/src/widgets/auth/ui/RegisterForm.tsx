import { Button, FormField, IconButton, Input } from '@shared'
import { Eye, EyeOff } from 'lucide-react'
import { useId } from 'react'
import { Controller } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { type RegisterSubmitHandler, useRegisterForm } from '../model'
import styles from './AuthForms.module.css'

interface RegisterFormProps {
  isPending?: boolean
  onSubmit?: RegisterSubmitHandler
  onSwitchToLogin?: () => void
}

export function RegisterForm({
  isPending = false,
  onSubmit,
  onSwitchToLogin,
}: RegisterFormProps) {
  const { t } = useTranslation()
  const nicknameInputId = useId()
  const emailInputId = useId()
  const passwordInputId = useId()
  const { form, isPasswordVisible, submit, togglePasswordVisibility } =
    useRegisterForm({ onSubmit })

  return (
    <section className={styles.formSection}>
      <header className={styles.header}>
        <h1 className={styles.title}>{t('Create account')}</h1>
        <p className={styles.subtitle}>
          {t('Create your workspace in less than a minute.')}
        </p>
      </header>
      <form className={styles.form} onSubmit={submit}>
        <div className={styles.formFields}>
          <Controller
            control={form.control}
            name="nickname"
            render={({ field, fieldState }) => (
              <FormField
                error={fieldState.error?.message}
                htmlFor={nicknameInputId}
                label={t('Nickname')}
                reserveErrorSpace
              >
                <Input
                  {...field}
                  autoComplete="name"
                  className={styles.inputControl}
                  invalid={Boolean(fieldState.error)}
                  id={nicknameInputId}
                  placeholder="alexkim"
                  ref={field.ref}
                />
              </FormField>
            )}
          />
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
                  autoComplete="new-password"
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
          {t('Create account')}
        </Button>
      </form>
      <p className={styles.footer}>
        {t('Already have an account?')}{' '}
        <button className={styles.link} onClick={onSwitchToLogin} type="button">
          {t('Sign in')}
        </button>
      </p>
      <p className={styles.legalNotice}>
        {t('By continuing, you agree to the Terms and Privacy Policy.')}
      </p>
    </section>
  )
}
