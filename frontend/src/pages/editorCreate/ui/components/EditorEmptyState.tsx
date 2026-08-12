import sparkles from '@shared/assets/sparkles.svg'
import { Button, Input } from '@shared/ui'

import { EditorCreateStateLayout } from './EditorCreateStateLayout'

interface EditorEmptyStateProps {
  description: string
  onDescriptionChange: (description: string) => void
  onGenerate: (draft: { description: string; name: string }) => void
}

export function EditorEmptyState({
  description,
  onDescriptionChange,
  onGenerate,
}: EditorEmptyStateProps) {
  return (
    <EditorCreateStateLayout
      composer={
        <>
          <Input
            leadingContent={<img alt="" src={sparkles} />}
            onChange={(event) => onDescriptionChange(event.target.value)}
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
