import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input, Portal } from '@shared/ui'
import { useEffect, useId } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'

import styles from './ProjectRenameModal.module.css'

interface ProjectRenameModalProps {
  isOpen: boolean
  onClose: () => void
  onSubmit: (projectName: string) => Promise<void>
  projectName: string
}

type ProjectRenameFormValues = { projectName: string }

export function ProjectRenameModal({
  isOpen,
  onClose,
  onSubmit,
  projectName,
}: ProjectRenameModalProps) {
  const { t } = useTranslation()
  const projectNameInputId = useId()
  const projectRenameFormSchema = z.object({
    projectName: z.string().trim().nonempty(t('Enter a project name.')),
  })
  const form = useForm<ProjectRenameFormValues>({
    defaultValues: { projectName },
    mode: 'onSubmit',
    resolver: zodResolver(projectRenameFormSchema),
  })

  useEffect(() => {
    if (isOpen) {
      form.reset({ projectName })
    }
  }, [form, isOpen, projectName])

  if (!isOpen) {
    return null
  }

  function handleClose() {
    form.reset({ projectName })
    onClose()
  }

  const submit = form.handleSubmit(async ({ projectName: nextProjectName }) => {
    await onSubmit(nextProjectName.trim())
  })

  return (
    <Portal>
      <div className={styles.overlay}>
        <button
          className={styles.backdrop}
          onClick={handleClose}
          type="button"
        />
        <section className={styles.modal}>
          <div className={styles.header}>
            <h2 className={styles.title}>{t('Rename project')}</h2>
          </div>
          <form className={styles.form} onSubmit={submit}>
            <FormField
              error={form.formState.errors.projectName?.message}
              htmlFor={projectNameInputId}
              label={t('Project name')}
            >
              <Input
                {...form.register('projectName')}
                autoFocus
                id={projectNameInputId}
                invalid={Boolean(form.formState.errors.projectName)}
              />
            </FormField>
            <div className={styles.actions}>
              <Button onClick={handleClose} variant="secondary">
                {t('Cancel')}
              </Button>
              <Button type="submit">{t('Save name')}</Button>
            </div>
          </form>
        </section>
      </div>
    </Portal>
  )
}
