import iconGlow from '@shared/assets/iconGlow.svg'
import { Button, Input } from '@shared/ui'

import styles from './EditorGeneratingState.module.css'

export function EditorGeneratingState() {
  return (
    <section className={styles.state}>
      {/* Позже выводить этап генерации и текст ошибки, полученные из mutation. */}
      <div className={styles.preview}>
        <div className={styles.copy}>
          <img alt="" className={styles.icon} src={iconGlow} />
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
