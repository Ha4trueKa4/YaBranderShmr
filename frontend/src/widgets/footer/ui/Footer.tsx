import { Select } from '@shared/ui'
import { Globe2, SunMoon } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { changeLanguage } from '@/i18n'
import { changeTheme, getInitialTheme } from '@/shared/lib/theme'

import styles from './Footer.module.css'

const languageOptions = [
  { code: 'en', label: 'English' },
  { code: 'ru', label: 'Русский' },
] as const

export function Footer() {
  const { i18n, t } = useTranslation()
  const language = i18n.resolvedLanguage === 'ru' ? 'ru' : 'en'
  const [theme, setTheme] = useState(getInitialTheme)

  return (
    <footer className={styles.footer}>
      <div className={styles.content}>
        <span className={styles.copyright}>© 2026 Ya Brander</span>
        <div className={styles.controls}>
          <div className={styles.control}>
            <SunMoon size={16} />
            <span>{t('Theme')}</span>
            <Select
              className={styles.select}
              menuPlacement="top"
              options={[
                { label: t('System'), value: 'system' },
                { label: t('Dark'), value: 'dark' },
                { label: t('Light'), value: 'light' },
              ]}
              value={theme}
              onChange={(value) => {
                if (
                  value === 'system' ||
                  value === 'dark' ||
                  value === 'light'
                ) {
                  changeTheme(value)
                  setTheme(value)
                }
              }}
            />
          </div>
          <div className={styles.control}>
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
      </div>
    </footer>
  )
}
