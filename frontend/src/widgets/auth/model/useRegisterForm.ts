import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { registerFormSchema } from './schemas'
import type { RegisterFormValues, RegisterSubmitHandler } from './types'

interface UseRegisterFormOptions {
  onSubmit?: RegisterSubmitHandler
}

export function useRegisterForm({ onSubmit }: UseRegisterFormOptions = {}) {
  const form = useForm<RegisterFormValues>({
    defaultValues: { email: '', nickname: '', password: '' },
    mode: 'onBlur',
    resolver: zodResolver(registerFormSchema),
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
