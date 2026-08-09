import { Button, Input } from '@shared/ui'
import { useState } from 'react'

import styles from './EditorEmptyState.module.css'

interface EditorEmptyStateProps {
  onGenerate: (draft: { description: string; name: string }) => void
}

export function EditorEmptyState({ onGenerate }: EditorEmptyStateProps) {
  const [description, setDescription] = useState('')

  return (
    <section className={styles.state}>
      <div className={styles.preview}>
        <div className={styles.copy}>
          <h1>Paste your description and generate</h1>
          <p>
            Create customized vectors, logos, and high-fidelity marks instantly.
          </p>
        </div>
      </div>
      <div className={styles.composer}>
        {/* Подключить контролируемое значение prompt и передать его в onGenerate. */}
        <Input
          onChange={(event) => setDescription(event.target.value)}
          placeholder="e.g. Modern abstract technology logo with overlapping gradients…"
          value={description}
        />
        <Button
          onClick={() => onGenerate({ description, name: 'Untitled project' })}
        >
          Generate
        </Button>
      </div>
    </section>
  )
}
