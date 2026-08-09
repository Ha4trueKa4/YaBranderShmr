import { Button, Input } from '@shared/ui'

import styles from './PromptComposer.module.css'

export function PromptComposer() {
  return (
    <section className={styles.composer}>
      {/* Здесь будет feature редактирования через AI: prompt, mutation и новая версия документа. */}
      <Input disabled placeholder="Describe what you want to create…" />
      <Button disabled size="md">
        Generate
      </Button>
    </section>
  )
}
