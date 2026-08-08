import type { SubmitHandler } from 'react-hook-form'

export interface LoginFormValues {
  email: string
  password: string
}

export interface RegisterFormValues extends LoginFormValues {
  fullName: string
}

export type LoginSubmitHandler = SubmitHandler<LoginFormValues>
export type RegisterSubmitHandler = SubmitHandler<RegisterFormValues>
