import { zodResolver } from '@hookform/resolvers/zod'
import type { Project } from '@shared/api/types'
import { useEscAction, useLockBodyScroll } from '@shared/lib'
import { Button, FormField, IconButton, Input, Portal } from '@shared/ui'
import { X } from 'lucide-react'
import { useEffect, useId, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'

import styles from './ProjectManagementModal.module.css'

type ProjectNameFormValues = { name: string }

interface ProjectManagementModalProps {
  onClose: () => void
  onOpenVersionHistory: () => void
  onSubmit: (values: { isPublic: boolean; name: string }) => Promise<void>
  project: Project
}

export function ProjectManagementModal({
  onClose,
  onOpenVersionHistory,
  onSubmit,
  project,
}: ProjectManagementModalProps) {
  useLockBodyScroll(true)
  useEscAction(onClose)

  const { t } = useTranslation()
  const nameInputId = useId()
  const [isPublic, setIsPublic] = useState(project.is_public ?? false)
  const projectNameFormSchema = z.object({
    name: z.string().trim().nonempty(t('Enter a project name.')),
  })
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
          <header className={styles.header}>
            <h2 className={styles.title}>{t('Manage project')}</h2>
            <IconButton
              className={styles.closeButton}
              onClick={onClose}
              size="sm"
            >
              <X />
            </IconButton>
          </header>
          <form
            className={styles.form}
            onSubmit={form.handleSubmit(async ({ name }) => {
              await onSubmit({ isPublic, name })
            })}
          >
            <FormField
              error={form.formState.errors.name?.message}
              htmlFor={nameInputId}
              label={t('Project name')}
            >
              <Input
                {...form.register('name')}
                autoFocus
                id={nameInputId}
                invalid={Boolean(form.formState.errors.name)}
              />
            </FormField>
            <div className={styles.visibility}>
              <span>{t('Visibility')}</span>
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
                  {t('Public')}
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
                  {t('Private')}
                </Button>
              </div>
            </div>
            <div className={styles.visibility}>
              <span>{t('Versions')}</span>
              <Button onClick={onOpenVersionHistory} variant="outline">
                {t('Version history')}
              </Button>
            </div>
            <div className={styles.actions}>
              <Button onClick={onClose} variant="secondary">
                {t('Cancel')}
              </Button>
              <Button loading={form.formState.isSubmitting} type="submit">
                {t('Save')}
              </Button>
            </div>
          </form>
        </section>
      </div>
    </Portal>
  )
}
