import styles from '../EditorMobileLayout.module.css'

interface MobileSelectionActionsProps {
  fillColor: string | null
  hasSelection: boolean
  onOpenColorPicker: () => void
}

export function MobileSelectionActions({
  fillColor,
  hasSelection,
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
          <button className={styles.deleteAction} type="button">
            Delete
          </button>
        </>
      ) : (
        <p>Select an element</p>
      )}
    </div>
  )
}
