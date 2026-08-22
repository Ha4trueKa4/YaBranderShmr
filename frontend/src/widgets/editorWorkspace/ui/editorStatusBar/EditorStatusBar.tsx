import type { EditorNode } from '@entities/editorDocument'
import { useTranslation } from 'react-i18next'
import styles from './EditorStatusBar.module.css'

interface EditorStatusBarProps {
  selectedNode: EditorNode | null
  zoom: number
}

export function EditorStatusBar({ selectedNode, zoom }: EditorStatusBarProps) {
  const { t } = useTranslation()
  const layerName = selectedNode?.name ?? t('No layer selected')
  const zoomPercent = Math.round(zoom * 100)

  return (
    <footer className={styles.status}>
      {t('{{layerName}} · {{zoom}}%', { layerName, zoom: zoomPercent })}
    </footer>
  )
}
