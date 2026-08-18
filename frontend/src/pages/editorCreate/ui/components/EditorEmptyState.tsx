import { GenerationPromptComposer } from '@features/editorGenerate'
import { Controller } from 'react-hook-form'

import { useGenerationForm } from '../../model/useGenerationForm'
import { EditorCreateStateLayout } from './EditorCreateStateLayout'
import styles from './EditorEmptyState.module.css'

interface EditorEmptyStateProps {
  onGenerate: (draft: {
    description: string
    model: string
    name: string
  }) => void
  onUnauthorizedModelSelect?: () => void
}

export function EditorEmptyState({
  onGenerate,
  onUnauthorizedModelSelect,
}: EditorEmptyStateProps) {
  const { form, submit } = useGenerationForm({
    onSubmit: ({ description, model }) =>
      onGenerate({ description, model, name: 'Untitled project' }),
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
      description="Create customized vectors, logos, and high-fidelity marks instantly."
      isCopyWidthLimited
      isPreviewCompact
      title="Paste your description and generate"
    />
  )
}
