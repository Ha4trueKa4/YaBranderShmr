import { GenerationPromptComposer } from '@features/editorGenerate'

import { EditorCreateStateLayout } from './EditorCreateStateLayout'

interface EditorGeneratingStateProps {
  description: string
  model: string
}

export function EditorGeneratingState({
  description,
  model,
}: EditorGeneratingStateProps) {
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
      description="This usually takes a few seconds."
      title="Creating your identity…"
    />
  )
}
