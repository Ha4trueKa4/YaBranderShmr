import { z } from 'zod'

export const passwordSchema = z
  .string()
  .min(8, 'Password must be at least 8 characters.')
  .max(128, 'Password must be no longer than 128 characters.')
  .regex(/[A-Za-z]/, 'Password must contain a letter.')
  .regex(/\d/, 'Password must contain a digit.')

export const loginFormSchema = z.object({
  email: z.email('Enter a valid email address.'),
  password: passwordSchema,
})

export const registerFormSchema = loginFormSchema.extend({
  nickname: z.string().nonempty('Nickname is required.'),
})
