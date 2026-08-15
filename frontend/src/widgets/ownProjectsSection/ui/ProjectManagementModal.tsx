import { zodResolver } from '@hookform/resolvers/zod'
import type { Project } from '@shared/api/types'
import { Button, FormField, Input, Portal } from '@shared/ui'
import { useEffect, useId, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import styles from './ProjectManagementModal.module.css'

const projectNameFormSchema = z.object({
  name: z.string().trim().nonempty('Enter a project name.'),
})

type ProjectNameFormValues = z.infer<typeof projectNameFormSchema>

interface ProjectManagementModalProps {
  onClose: () => void
  onSubmit: (values: { isPublic: boolean; name: string }) => void
  project: Project
}

export function ProjectManagementModal({
  onClose,
  onSubmit,
  project,
}: ProjectManagementModalProps) {
  const nameInputId = useId()
  const [isPublic, setIsPublic] = useState(project.is_public ?? false)
  const form = useForm<ProjectNameFormValues>({
    defaultValues: { name: project.name ?? '' },
    mode: 'onSubmit',
    resolver: zodResolver(projectNameFormSchema),
  })

  useEffect(() => {
    form.reset({ name: project.name ?? '' })
    setIsPublic(project.is_public ?? false)
  }, [form, project])

  return (
    <Portal>
      <div className={styles.overlay}>
        <button className={styles.backdrop} onClick={onClose} type="button" />
        <section className={styles.modal}>
          <h2 className={styles.title}>Manage project</h2>
          <form
            className={styles.form}
            onSubmit={form.handleSubmit(({ name }) => {
              onSubmit({ isPublic, name })
            })}
          >
            <FormField
              error={form.formState.errors.name?.message}
              htmlFor={nameInputId}
              label="Project name"
            >
              <Input
                {...form.register('name')}
                autoFocus
                id={nameInputId}
                invalid={Boolean(form.formState.errors.name)}
              />
            </FormField>
            <div className={styles.visibility}>
              <span>Visibility</span>
              <div className={styles.visibilityOptions}>
                <Button
                  className={
                    isPublic ? styles.selectedVisibilityOption : undefined
                  }
                  onClick={() => setIsPublic(true)}
                  size="sm"
                  type="button"
                  variant="secondary"
                >
                  Public
                </Button>
                <Button
                  className={
                    !isPublic ? styles.selectedVisibilityOption : undefined
                  }
                  onClick={() => setIsPublic(false)}
                  size="sm"
                  type="button"
                  variant="secondary"
                >
                  Private
                </Button>
              </div>
            </div>
            <div className={styles.actions}>
              <Button onClick={onClose} variant="secondary">
                Cancel
              </Button>
              <Button loading={form.formState.isSubmitting} type="submit">
                Save
              </Button>
            </div>
          </form>
        </section>
      </div>
    </Portal>
  )
}
