import { z } from 'zod'

export const loginFormSchema = z.object({
  email: z.string().min(1, 'Введите email.').email('Введите корректный email.'),
  password: z.string().nonempty('Введите пароль'),
})

export const registerFormSchema = loginFormSchema.extend({
  nickname: z.string().nonempty('Введите имя'),
})
