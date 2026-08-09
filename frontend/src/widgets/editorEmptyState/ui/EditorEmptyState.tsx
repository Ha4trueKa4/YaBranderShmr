import { Button, Input } from '@shared/ui'

import styles from './EditorEmptyState.module.css'

interface EditorEmptyStateProps {
  onGenerate: () => void
}

export function EditorEmptyState({ onGenerate }: EditorEmptyStateProps) {
  return (
    <section className={styles.state}>
      <div className={styles.preview}>
        <div className={styles.copy}>
          <h1>Paste your description and generate</h1>
          <p>Create customized vectors, logos, and high-fidelity marks instantly.</p>
        </div>
      </div>
      <div className={styles.composer}>
        {/* Подключить контролируемое значение prompt и передать его в onGenerate. */}
        <Input placeholder="e.g. Modern abstract technology logo with overlapping gradients…" />
        <Button onClick={onGenerate}>Generate</Button>
      </div>
    </section>
  )
}
