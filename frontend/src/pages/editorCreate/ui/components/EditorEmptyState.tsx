import sparkles from '@shared/assets/sparkles.svg'
import { Button, Input } from '@shared/ui'
import { Controller } from 'react-hook-form'

import { useGenerationForm } from '../../model/useGenerationForm'
import { EditorCreateStateLayout } from './EditorCreateStateLayout'
import styles from './EditorEmptyState.module.css'

interface EditorEmptyStateProps {
  onGenerate: (draft: { description: string; name: string }) => void
}

export function EditorEmptyState({ onGenerate }: EditorEmptyStateProps) {
  const { form, submit } = useGenerationForm({
    onSubmit: ({ description }) =>
      onGenerate({ description, name: 'Untitled project' }),
  })

  return (
    <EditorCreateStateLayout
      composer={
        <form className={styles.form} onSubmit={submit}>
          <Controller
            control={form.control}
            name="description"
            render={({ field, fieldState }) => (
              <div className={styles.descriptionField}>
                <div className={styles.inputRow}>
                  <Input
                    {...field}
                    className={styles.input}
                    invalid={Boolean(fieldState.error)}
                    leadingContent={<img alt="" src={sparkles} />}
                    placeholder="e.g. Modern abstract technology logo with overlapping gradients…"
                    ref={field.ref}
                  />
                  <Button type="submit">Generate</Button>
                </div>
                {fieldState.error && (
                  <p className={styles.error}>{fieldState.error.message}</p>
                )}
              </div>
            )}
          />
        </form>
      }
      description="Create customized vectors, logos, and high-fidelity marks instantly."
      isCopyWidthLimited
      title="Paste your description and generate"
    />
  )
}
