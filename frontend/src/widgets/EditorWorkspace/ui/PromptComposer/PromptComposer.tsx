import { Button, Input } from '@shared/ui'

import styles from './PromptComposer.module.css'

export function PromptComposer() {
  return (
    <section className={styles.composer}>
      <Input disabled placeholder="Describe what you want to create…" />
      <Button disabled size="md">Generate</Button>
    </section>
  )
}
