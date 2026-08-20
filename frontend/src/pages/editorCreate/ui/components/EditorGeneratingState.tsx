import { GenerationPromptComposer } from '@features/editorGenerate'
import { useTranslation } from 'react-i18next'

import { EditorCreateStateLayout } from './EditorCreateStateLayout'

interface EditorGeneratingStateProps {
  description: string
  model: string
}

export function EditorGeneratingState({
  description,
  model,
}: EditorGeneratingStateProps) {
  const { t } = useTranslation()
  return (
    <EditorCreateStateLayout
      composer={
        <GenerationPromptComposer
          description={description}
          isGenerating
          model={model}
          onDescriptionChange={() => undefined}
          onGenerate={() => undefined}
          onModelChange={() => undefined}
        />
      }
      description={t('This usually takes a few seconds.')}
      title={t('Creating your identity…')}
    />
  )
}
