import { Button, FormField, IconButton, Input } from '@shared'
import { Eye, EyeOff } from 'lucide-react'
import { useId } from 'react'
import { Controller } from 'react-hook-form'
import { type LoginSubmitHandler, useLoginForm } from '../model'
import styles from './AuthForms.module.css'

interface LoginFormProps {
  onSubmit?: LoginSubmitHandler
  onSwitchToRegister?: () => void
}

export function LoginForm({ onSubmit, onSwitchToRegister }: LoginFormProps) {
  const emailInputId = useId()
  const passwordInputId = useId()
  const {
    form,
    isPasswordVisible,
    isSubmitted,
    submit,
    togglePasswordVisibility,
  } = useLoginForm({ onSubmit })

  return (
    <section className={styles.formSection}>
      <header className={styles.header}>
        <h1 className={styles.title}>Welcome back</h1>
        <p className={styles.subtitle}>Sign in to continue creating.</p>
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
                label="Email"
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
                label="Password"
                reserveErrorSpace
              >
                <Input
                  {...field}
                  autoComplete="current-password"
                  className={styles.inputControl}
                  invalid={Boolean(fieldState.error)}
                  id={passwordInputId}
                  placeholder="Enter your password"
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
        <Button fullWidth size="lg" type="submit">
          Sign in
        </Button>
        {isSubmitted && <p className={styles.success}>You are signed in.</p>}
      </form>
      <p className={styles.footer}>
        New to Brandkit AI?{' '}
        <button
          className={styles.link}
          onClick={onSwitchToRegister}
          type="button"
        >
          Create account
        </button>
      </p>
    </section>
  )
}
