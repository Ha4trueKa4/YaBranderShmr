import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input, Portal } from '@shared/ui'
import { useEffect, useId } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import styles from './EditProfileModal.module.css'

const editProfileFormSchema = z.object({
  name: z.string().trim().nonempty('Enter a display name.'),
})

type EditProfileFormValues = z.infer<typeof editProfileFormSchema>

interface EditProfileModalProps {
  isOpen: boolean
  name: string
  onClose: () => void
  onSubmit: (name: string) => void
}

export function EditProfileModal({
  isOpen,
  name,
  onClose,
  onSubmit,
}: EditProfileModalProps) {
  const nameInputId = useId()
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
          <h2 className={styles.title}>Edit profile</h2>
          <form
            className={styles.form}
            onSubmit={form.handleSubmit(({ name: nextName }) => {
              onSubmit(nextName)
            })}
          >
            <FormField
              error={form.formState.errors.name?.message}
              htmlFor={nameInputId}
              label="Display name"
              required
            >
              <Input
                {...form.register('name')}
                autoFocus
                id={nameInputId}
                invalid={Boolean(form.formState.errors.name)}
              />
            </FormField>
            <div className={styles.actions}>
              <Button onClick={onClose} variant="secondary">
                Cancel
              </Button>
              <Button type="submit">Save changes</Button>
            </div>
          </form>
        </section>
      </div>
    </Portal>
  )
}
