import { useTranslation } from 'react-i18next'
import styles from '../EditorMobileLayout.module.css'

interface MobileSelectionActionsProps {
  canDeleteSelection: boolean
  fillColor: string | null
  hasSelection: boolean
  onDeleteSelection: () => void
  onOpenColorPicker: () => void
}

export function MobileSelectionActions({
  canDeleteSelection,
  fillColor,
  hasSelection,
  onDeleteSelection,
  onOpenColorPicker,
}: MobileSelectionActionsProps) {
  const { t } = useTranslation()
  return (
    <div className={styles.selectionActions}>
      {hasSelection ? (
        <>
          <button
            disabled={!fillColor}
            onClick={onOpenColorPicker}
            type="button"
          >
            <span
              className={styles.fillDot}
              style={{ background: fillColor ?? undefined }}
            />
            {t('Fill')}
          </button>
          <span />
          <button type="button">{t('Stroke')}</button>
          <span />
          <button
            className={styles.deleteAction}
            disabled={!canDeleteSelection}
            onClick={onDeleteSelection}
            type="button"
          >
            {t('Delete')}
          </button>
        </>
      ) : (
        <p>{t('Select an element')}</p>
      )}
    </div>
  )
}
