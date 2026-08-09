import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { loginFormSchema } from './schemas'
import type { LoginFormValues, LoginSubmitHandler } from './types'

interface UseLoginFormOptions {
  onSubmit?: LoginSubmitHandler
}

export function useLoginForm({ onSubmit }: UseLoginFormOptions = {}) {
  const form = useForm<LoginFormValues>({
    defaultValues: { email: '', password: '' },
    mode: 'onBlur',
    resolver: zodResolver(loginFormSchema),
  })
  const [isPasswordVisible, setPasswordVisible] = useState(false)
  const [isSubmitted, setSubmitted] = useState(false)

  const submit = form.handleSubmit(async (values) => {
    await onSubmit?.(values)
    setSubmitted(true)
  })

  return {
    form,
    isPasswordVisible,
    isSubmitted,
    submit,
    togglePasswordVisibility: () =>
      setPasswordVisible((isVisible) => !isVisible),
  }
}
