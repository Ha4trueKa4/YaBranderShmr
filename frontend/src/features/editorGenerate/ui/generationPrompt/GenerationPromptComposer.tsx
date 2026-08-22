import { Button } from '@shared/ui'
import { Sparkles } from 'lucide-react'
import { useLayoutEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'

import { DEFAULT_GENERATION_MODEL, ModelSelect } from '../modelSelect'
import styles from './GenerationPrompt.module.css'

const MAX_PROMPT_LENGTH = 1500

interface GenerationPromptComposerProps {
  description: string
  error?: string
  isGenerating?: boolean
  model: string
  onDescriptionChange: (description: string) => void
  onGenerate: () => void
  onModelChange: (model: string) => void
  onUnauthorizedModelSelect?: () => void
}

export function GenerationPromptComposer({
  description,
  error,
  isGenerating = false,
  model,
  onDescriptionChange,
  onGenerate,
  onModelChange,
  onUnauthorizedModelSelect,
}: GenerationPromptComposerProps) {
  const { t } = useTranslation()
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  useLayoutEffect(() => {
    const textarea = textareaRef.current

    if (!textarea) {
      return
    }

    textarea.style.height = 'auto'
    textarea.style.height = `${Math.min(textarea.scrollHeight, 192)}px`
  })

  return (
    <div className={styles.field}>
      <div className={styles.composer}>
        <Sparkles className={styles.icon} size={18} strokeWidth={2} />
        <textarea
          className={styles.textarea}
          disabled={isGenerating}
          onChange={(event) => onDescriptionChange(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter' && !event.shiftKey) {
              event.preventDefault()
              onGenerate()
            }
          }}
          maxLength={MAX_PROMPT_LENGTH}
          placeholder={t(
            'e.g. Modern abstract technology logo with overlapping gradients…',
          )}
          ref={textareaRef}
          value={description}
        />
        <div className={styles.actions}>
          <span className={styles.characterCounter}>
            {description.length}/{MAX_PROMPT_LENGTH}
          </span>
          <div className={styles.actionControls}>
            <div className={styles.modelSelect}>
              <ModelSelect
                disabled={isGenerating}
                onChange={onModelChange}
                onUnauthorizedSelect={onUnauthorizedModelSelect}
                value={model || DEFAULT_GENERATION_MODEL}
                variant="compact"
              />
            </div>
            <Button
              className={styles.generateButton}
              loading={isGenerating}
              onClick={onGenerate}
              type="button"
            >
              {t('Generate')}
            </Button>
          </div>
        </div>
      </div>
      <p className={styles.error}>{error}</p>
    </div>
  )
}
