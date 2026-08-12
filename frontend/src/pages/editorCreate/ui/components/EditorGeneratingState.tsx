import { Button, Input } from '@shared/ui'

import { EditorCreateStateLayout } from './EditorCreateStateLayout'

interface EditorGeneratingStateProps {
  description: string
}

export function EditorGeneratingState({
  description,
}: EditorGeneratingStateProps) {
  return (
    <EditorCreateStateLayout
      composer={
        <>
          <Input disabled value={description} />
          <Button disabled loading>
            {null}
          </Button>
        </>
      }
      description="This usually takes a few seconds."
      title="Creating your identity…"
    />
  )
}
