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
    mode: 'onSubmit',
    resolver: zodResolver(loginFormSchema),
  })
  const [isPasswordVisible, setPasswordVisible] = useState(false)

  const submit = form.handleSubmit(async (values) => {
    await onSubmit?.(values)
  })

  return {
    form,
    isPasswordVisible,
    submit,
    togglePasswordVisibility: () =>
      setPasswordVisible((isVisible) => !isVisible),
  }
}
