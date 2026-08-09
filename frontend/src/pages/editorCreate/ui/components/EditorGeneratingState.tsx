import { Button, Input } from '@shared/ui'

import { EditorCreateStateLayout } from './EditorCreateStateLayout'

export function EditorGeneratingState() {
  return (
    <EditorCreateStateLayout
      composer={
        <>
          <Input disabled placeholder="Generating your brand identity…" />
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
