import { Button, FormField, IconButton, Input } from '@shared'
import { Eye, EyeOff } from 'lucide-react'
import { Controller } from 'react-hook-form'
import { type RegisterSubmitHandler, useRegisterForm } from '../model'
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
    <section className={styles.formSection}>
      <header className={styles.header}>
        <h1 className={styles.title}>Create account</h1>
        <p className={styles.subtitle}>
          Create your workspace in less than a minute.
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
                label="Nickname"
                reserveErrorSpace
              >
                <Input
                  {...field}
                  autoComplete="name"
                  className={styles.inputControl}
                  invalid={Boolean(fieldState.error)}
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
                label="Email"
                reserveErrorSpace
              >
                <Input
                  {...field}
                  autoComplete="email"
                  className={styles.inputControl}
                  invalid={Boolean(fieldState.error)}
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
                label="Password"
                reserveErrorSpace
              >
                <Input
                  {...field}
                  autoComplete="new-password"
                  className={styles.inputControl}
                  invalid={Boolean(fieldState.error)}
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
          Create account
        </Button>
        {isSubmitted && (
          <p className={styles.success}>Your account has been created.</p>
        )}
      </form>
      <p className={styles.footer}>
        Already have an account?{' '}
        <button className={styles.link} onClick={onSwitchToLogin} type="button">
          Sign in
        </button>
      </p>
      <p className={styles.legalNotice}>
        By continuing, you agree to the Terms and Privacy Policy.
      </p>
    </section>
  )
}
