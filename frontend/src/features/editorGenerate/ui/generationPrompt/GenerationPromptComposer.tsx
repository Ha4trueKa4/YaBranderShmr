import { Button, StopGeneratingIcon } from '@shared/ui'
import { ArrowUp, Sparkles } from 'lucide-react'
import { useLayoutEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'

import { DEFAULT_GENERATION_MODEL, ModelSelect } from '../modelSelect'
import styles from './GenerationPrompt.module.css'

const MAX_PROMPT_LENGTH = 1500

interface GenerationPromptComposerProps {
  description: string
  error?: string
  isCancelling?: boolean
  isGenerating?: boolean
  model: string
  onDescriptionChange: (description: string) => void
  onCancel?: () => void
  onGenerate: () => void
  onModelChange: (model: string) => void
  onUnauthorizedModelSelect?: () => void
}

export function GenerationPromptComposer({
  description,
  error,
  isCancelling = false,
  isGenerating = false,
  model,
  onDescriptionChange,
  onCancel,
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
            const isNewLineShortcut = event.metaKey || event.ctrlKey

            if (event.key === 'Enter' && isNewLineShortcut) {
              const { selectionEnd, selectionStart } = event.currentTarget
              const nextDescription = `${description.slice(0, selectionStart)}\n${description.slice(selectionEnd)}`

              event.preventDefault()

              if (nextDescription.length > MAX_PROMPT_LENGTH) {
                return
              }

              onDescriptionChange(nextDescription)
              requestAnimationFrame(() => {
                textareaRef.current?.setSelectionRange(
                  selectionStart + 1,
                  selectionStart + 1,
                )
              })
              return
            }

            if (
              event.key !== 'Enter' ||
              event.shiftKey ||
              event.nativeEvent.isComposing
            ) {
              return
            }

            event.preventDefault()
            onGenerate()
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
              aria-label={isGenerating ? t('Cancel generation') : t('Generate')}
              loading={isCancelling}
              onClick={isGenerating ? onCancel : onGenerate}
              type="button"
            >
              {isGenerating ? (
                <StopGeneratingIcon aria-hidden="true" />
              ) : (
                <ArrowUp aria-hidden="true" size={18} strokeWidth={2.5} />
              )}
            </Button>
          </div>
        </div>
      </div>
      <p className={styles.error}>{error}</p>
    </div>
  )
}
