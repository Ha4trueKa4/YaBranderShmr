import { Feed } from '@widgets'
import { Header } from '@widgets/header'

import styles from './feedPage.module.css'

export function FeedPage() {
  return (
    <div className={styles.page}>
      <Header />

      <main className={styles.content}>
        <section className={styles.intro}>
          <p className={styles.overline}>Discover creative work</p>
          <h1>Explore public identities</h1>
          <p className={styles.description}>
            Fresh brand systems, generated and refined by the community.
          </p>
        </section>
        <Feed />
      </main>
    </div>
  )
}
