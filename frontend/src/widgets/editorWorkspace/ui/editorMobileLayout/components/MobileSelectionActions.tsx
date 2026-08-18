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
            Fill
          </button>
          <span />
          <button type="button">Stroke</button>
          <span />
          <button
            className={styles.deleteAction}
            disabled={!canDeleteSelection}
            onClick={onDeleteSelection}
            type="button"
          >
            Delete
          </button>
        </>
      ) : (
        <p>Select an element</p>
      )}
    </div>
  )
}
