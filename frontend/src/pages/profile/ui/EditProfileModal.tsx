import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input, Portal } from '@shared/ui'
import { useEffect, useId } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'

import styles from './EditProfileModal.module.css'

type EditProfileFormValues = { name: string }

interface EditProfileModalProps {
  isSubmitting: boolean
  isOpen: boolean
  name: string
  onClose: () => void
  onSubmit: (name: string) => Promise<void>
}

export function EditProfileModal({
  isSubmitting,
  isOpen,
  name,
  onClose,
  onSubmit,
}: EditProfileModalProps) {
  const { t } = useTranslation()
  const nameInputId = useId()
  const editProfileFormSchema = z.object({
    name: z
      .string()
      .trim()
      .min(3, t('Display name must be 3-64 characters.'))
      .max(64, t('Display name must be 3-64 characters.')),
  })
  const form = useForm<EditProfileFormValues>({
    defaultValues: { name },
    mode: 'onSubmit',
    resolver: zodResolver(editProfileFormSchema),
  })

  useEffect(() => {
    if (isOpen) {
      form.reset({ name })
    }
  }, [form, isOpen, name])

  if (!isOpen) {
    return null
  }

  return (
    <Portal>
      <div className={styles.overlay}>
        <button className={styles.backdrop} onClick={onClose} type="button" />
        <section className={styles.modal}>
          <h2 className={styles.title}>{t('Edit profile')}</h2>
          <form
            className={styles.form}
            onSubmit={form.handleSubmit(async ({ name: nextName }) => {
              await onSubmit(nextName)
            })}
          >
            <FormField
              error={form.formState.errors.name?.message}
              htmlFor={nameInputId}
              label={t('Display name')}
            >
              <Input
                {...form.register('name')}
                autoFocus
                id={nameInputId}
                invalid={Boolean(form.formState.errors.name)}
              />
            </FormField>
            <div className={styles.actions}>
              <Button
                disabled={isSubmitting}
                onClick={onClose}
                variant="secondary"
              >
                {t('Cancel')}
              </Button>
              <Button disabled={isSubmitting} type="submit">
                {t('Save changes')}
              </Button>
            </div>
          </form>
        </section>
      </div>
    </Portal>
  )
}
