import { Feed, Footer } from '@widgets'
import { Header } from '@widgets/header'
import { useTranslation } from 'react-i18next'

import styles from './feedPage.module.css'

export function FeedPage() {
  const { t } = useTranslation()

  return (
    <div className={styles.page}>
      <Header />

      <main className={styles.content}>
        <section className={styles.intro}>
          <p className={styles.overline}>{t('Discover creative work')}</p>
          <h1>{t('Explore public identities')}</h1>
          <p className={styles.description}>
            {t('Fresh brand systems, generated and refined by the community.')}
          </p>
        </section>
        <Feed />
      </main>
      <Footer />
    </div>
  )
}
