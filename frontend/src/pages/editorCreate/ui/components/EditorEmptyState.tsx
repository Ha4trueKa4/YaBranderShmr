import { GenerationPromptComposer } from '@features/editorGenerate'
import { useState } from 'react'
import { Controller } from 'react-hook-form'
import { useTranslation } from 'react-i18next'

import { useGenerationForm } from '../../model/useGenerationForm'
import { EditorCreateStateLayout } from './EditorCreateStateLayout'
import styles from './EditorEmptyState.module.css'
import { EditorFileDropzone } from './EditorFileDropzone'

interface EditorEmptyStateProps {
  initialDescription?: string
  initialModel?: string
  onUpload: (data: string) => void
  onGenerate: (draft: {
    description: string
    model: string
    name: string
  }) => void
  onUnauthorizedModelSelect?: () => void
}

export function EditorEmptyState({
  initialDescription,
  initialModel,
  onGenerate,
  onUpload,
  onUnauthorizedModelSelect,
}: EditorEmptyStateProps) {
  const { t } = useTranslation()
  const [isFileDragging, setIsFileDragging] = useState(false)
  const { form, submit } = useGenerationForm({
    initialValues: {
      description: initialDescription,
      model: initialModel,
    },
    onSubmit: ({ description, model }) =>
      onGenerate({ description, model, name: t('Untitled project') }),
  })

  return (
    <EditorCreateStateLayout
      composer={
        <form className={styles.form} onSubmit={submit}>
          <Controller
            control={form.control}
            name="description"
            render={({ field, fieldState }) => (
              <Controller
                control={form.control}
                name="model"
                render={({ field: modelField }) => (
                  <GenerationPromptComposer
                    description={field.value}
                    error={fieldState.error?.message}
                    model={modelField.value}
                    onDescriptionChange={field.onChange}
                    onGenerate={submit}
                    onModelChange={modelField.onChange}
                    onUnauthorizedModelSelect={onUnauthorizedModelSelect}
                  />
                )}
              />
            )}
          />
        </form>
      }
      description={t(
        'Create customized vectors, logos, and high-fidelity marks instantly.',
      )}
      isCopyWidthLimited
      isPreviewCompact
      isPreviewDragging={isFileDragging}
      preview={
        <EditorFileDropzone
          description={t(
            'Create customized vectors, logos, and high-fidelity marks instantly.',
          )}
          onDraggingChange={setIsFileDragging}
          onUpload={onUpload}
          title={t('Paste your description and generate')}
        />
      }
      title={t('Paste your description and generate')}
    />
  )
}
