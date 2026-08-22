import { BottomSheet, Button, Select } from '@shared/ui'
import { Globe2, SlidersHorizontal, SunMoon } from 'lucide-react'
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
  const [isSettingsOpen, setIsSettingsOpen] = useState(false)
  const themeOptions = [
    { label: t('Yandex Colors'), value: 'yandex' },
    { label: t('System'), value: 'system' },
    { label: t('Dark'), value: 'dark' },
    { label: t('Light'), value: 'light' },
    { label: t('Sephia Colors'), value: 'sephia' },
  ]
  const languageSelectOptions = languageOptions.map((option) => ({
    label: option.label,
    value: option.code,
  }))

  function handleThemeChange(value: string) {
    if (
      value === 'system' ||
      value === 'dark' ||
      value === 'light' ||
      value === 'sephia' ||
      value === 'yandex'
    ) {
      changeTheme(value)
      setTheme(value)
    }
  }

  function handleLanguageChange(value: string) {
    if (value === 'en' || value === 'ru') {
      void changeLanguage(value)
    }
  }

  return (
    <footer className={styles.footer}>
      <div className={styles.content}>
        <div className={styles.desktopControls}>
          <div className={styles.control}>
            <SunMoon size={16} />
            <span>{t('Theme')}</span>
            <Select
              className={styles.select}
              menuPlacement="top"
              options={themeOptions}
              value={theme}
              onChange={handleThemeChange}
            />
          </div>
          <div className={styles.control}>
            <Globe2 size={16} />
            <span>{t('Language')}</span>
            <Select
              className={styles.select}
              menuPlacement="top"
              options={languageSelectOptions}
              value={language}
              onChange={handleLanguageChange}
            />
          </div>
        </div>
        <span className={styles.copyright}>© 2026 Ya Brander</span>
        <Button
          className={styles.mobileSettings}
          size="sm"
          variant="ghost"
          onClick={() => setIsSettingsOpen(true)}
        >
          <SlidersHorizontal size={16} />
          {t('Settings')}
        </Button>
      </div>
      <BottomSheet
        className={styles.settingsSheet}
        contentClassName={styles.settingsSheetContent}
        isOpen={isSettingsOpen}
        title={t('Settings')}
        onClose={() => setIsSettingsOpen(false)}
      >
        <div className={styles.sheetControls}>
          <div className={[styles.control, styles.sheetControl].join(' ')}>
            <SunMoon size={16} />
            <span>{t('Theme')}</span>
            <Select
              className={styles.select}
              menuPlacement="top"
              options={themeOptions}
              value={theme}
              onChange={handleThemeChange}
            />
          </div>
          <div className={[styles.control, styles.sheetControl].join(' ')}>
            <Globe2 size={16} />
            <span>{t('Language')}</span>
            <Select
              className={styles.select}
              menuPlacement="top"
              options={languageSelectOptions}
              value={language}
              onChange={handleLanguageChange}
            />
          </div>
        </div>
      </BottomSheet>
    </footer>
  )
}
