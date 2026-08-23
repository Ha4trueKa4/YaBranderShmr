import { IconButton, Input } from '@shared/ui'
import { Send } from 'lucide-react'
import type { FormEvent, KeyboardEvent } from 'react'
import { useTranslation } from 'react-i18next'
import styles from './ChatComposer.module.css'

interface ChatComposerProps {
  disabled?: boolean
  onChange: (nextValue: string) => void
  onSubmit: () => void
  placeholder: string
  value: string
}

export function ChatComposer({
  disabled = false,
  onChange,
  onSubmit,
  placeholder,
  value,
}: ChatComposerProps) {
  const { t } = useTranslation()
  const isSubmitDisabled = disabled || !value.trim()

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!isSubmitDisabled) {
      onSubmit()
    }
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter' && !event.nativeEvent.isComposing) {
      event.preventDefault()

      if (!isSubmitDisabled) {
        onSubmit()
      }
    }
  }

  return (
    <form className={styles.composer} onSubmit={handleSubmit}>
      <Input
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        value={value}
      />
      <IconButton
        disabled={isSubmitDisabled}
        title={t('Send message')}
        type="submit"
      >
        <Send size={17} strokeWidth={2} />
      </IconButton>
    </form>
  )
}
