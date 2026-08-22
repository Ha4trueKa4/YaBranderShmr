import { useTranslation } from 'react-i18next'
import styles from './EditorStatusBar.module.css'

export function EditorStatusBar() {
  const { t } = useTranslation()
  // Подставить selectedLayer, размеры артборда и zoom из editorStore.
  return (
    <footer className={styles.status}>{t('No layer selected · 100%')}</footer>
  )
}
