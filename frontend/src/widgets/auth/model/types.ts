import type { SubmitHandler } from 'react-hook-form'
import type { z } from 'zod'
import type { loginFormSchema, registerFormSchema } from './schemas'

export type LoginFormValues = z.infer<typeof loginFormSchema>
export type RegisterFormValues = z.infer<typeof registerFormSchema>

export type LoginSubmitHandler = SubmitHandler<LoginFormValues>
export type RegisterSubmitHandler = SubmitHandler<RegisterFormValues>
