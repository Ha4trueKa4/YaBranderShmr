import { GenerationPromptComposer } from '@features/editorGenerate'
import { useTranslation } from 'react-i18next'

import { EditorCreateStateLayout } from './EditorCreateStateLayout'

interface EditorGeneratingStateProps {
  description: string
  isCancelling: boolean
  model: string
  onCancel: () => void
}

export function EditorGeneratingState({
  description,
  isCancelling,
  model,
  onCancel,
}: EditorGeneratingStateProps) {
  const { t } = useTranslation()
  return (
    <EditorCreateStateLayout
      composer={
        <GenerationPromptComposer
          description={description}
          isCancelling={isCancelling}
          isGenerating
          model={model}
          onDescriptionChange={() => undefined}
          onCancel={onCancel}
          onGenerate={() => undefined}
          onModelChange={() => undefined}
        />
      }
      description={t('This usually takes a few seconds.')}
      isTitleAnimated
      title={t('Creating your identity…')}
    />
  )
}
