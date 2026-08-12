import { z } from 'zod'

export const loginFormSchema = z.object({
  email: z
    .string()
    .min(1, 'Email is required.')
    .email('Enter a valid email address.'),
  password: z.string().nonempty('Password is required.'),
})

export const registerFormSchema = loginFormSchema.extend({
  nickname: z.string().nonempty('Nickname is required.'),
})
