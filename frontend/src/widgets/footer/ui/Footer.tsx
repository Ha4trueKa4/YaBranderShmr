import { Select } from '@shared/ui'
import { Globe2 } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { changeLanguage } from '@/i18n'

import styles from './Footer.module.css'

const languageOptions = [
  { code: 'en', label: 'English' },
  { code: 'ru', label: 'Русский' },
] as const

export function Footer() {
  const { i18n, t } = useTranslation()
  const language = i18n.resolvedLanguage === 'ru' ? 'ru' : 'en'

  return (
    <footer className={styles.footer}>
      <div className={styles.content}>
        <span className={styles.copyright}>© 2026 Ya Brander Y&&Y</span>
        <div className={styles.languageControl}>
          <Globe2 size={16} />
          <span>{t('Language')}</span>
          <Select
            className={styles.select}
            menuPlacement="top"
            options={languageOptions.map((option) => ({
              label: option.label,
              value: option.code,
            }))}
            value={language}
            onChange={(value) => {
              if (value === 'en' || value === 'ru') {
                void changeLanguage(value)
              }
            }}
          />
        </div>
      </div>
    </footer>
  )
}
