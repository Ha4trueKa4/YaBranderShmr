import { z } from 'zod'
import i18n from '@/i18n'

export const passwordSchema = z
  .string()
  .min(8, i18n.t('Password must be at least 8 characters.'))
  .max(128, i18n.t('Password must be no longer than 128 characters.'))
  .regex(/[A-Za-z]/, i18n.t('Password must contain a letter.'))
  .regex(/\d/, i18n.t('Password must contain a digit.'))

export const loginFormSchema = z.object({
  email: z.email(i18n.t('Enter a valid email address.')),
  password: passwordSchema,
})

export const registerFormSchema = loginFormSchema.extend({
  nickname: z.string().nonempty(i18n.t('Nickname is required.')),
})
