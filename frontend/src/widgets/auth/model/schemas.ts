import { z } from 'zod'

export const loginFormSchema = z.object({
  email: z.string().min(1, 'Введите email.').email('Введите корректный email.'),
  password: z
    .string()
    .min(1, 'Введите пароль.')
    .min(8, 'Пароль должен содержать минимум 8 символов.'),
})

export const registerFormSchema = loginFormSchema.extend({
  fullName: z
    .string()
    .min(1, 'Введите имя.')
    .min(2, 'Введите минимум 2 символа.'),
})
