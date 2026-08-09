import { Button, Input } from '@shared/ui'

import styles from './EditorGeneratingState.module.css'

export function EditorGeneratingState() {
  return (
    <section className={styles.state}>
      <div className={styles.preview}>
        <div className={styles.copy}>
          <h1>Creating your identity…</h1>
          <p>This usually takes a few seconds.</p>
        </div>
      </div>
      <div className={styles.composer}>
        <Input disabled placeholder="Generating your brand identity…" />
        <Button disabled loading>
          {null}
        </Button>
      </div>
    </section>
  )
}
