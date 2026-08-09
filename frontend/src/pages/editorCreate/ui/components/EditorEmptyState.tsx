import sparkles from '@shared/assets/sparkles.svg'
import { Button, Input } from '@shared/ui'
import { useState } from 'react'

import { EditorCreateStateLayout } from './EditorCreateStateLayout'

interface EditorEmptyStateProps {
  onGenerate: (draft: { description: string; name: string }) => void
}

export function EditorEmptyState({ onGenerate }: EditorEmptyStateProps) {
  const [description, setDescription] = useState('')

  return (
    <EditorCreateStateLayout
      composer={
        <>
          {/* Подключить контролируемое значение prompt и передать его в onGenerate. */}
          <Input
            leadingContent={<img alt="" src={sparkles} />}
            onChange={(event) => setDescription(event.target.value)}
            placeholder="e.g. Modern abstract technology logo with overlapping gradients…"
            value={description}
          />
          <Button
            onClick={() =>
              onGenerate({ description, name: 'Untitled project' })
            }
          >
            Generate
          </Button>
        </>
      }
      description="Create customized vectors, logos, and high-fidelity marks instantly."
      isCopyWidthLimited
      title="Paste your description and generate"
    />
  )
}
