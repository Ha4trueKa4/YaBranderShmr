import {
  BottomSheet,
  ColorPicker,
  ColorSwatch,
  IconButton,
  PopupMenu,
} from '@shared/ui'
import type { ComponentProps, ReactNode } from 'react'
import { useState } from 'react'

import iconHand from '../editorToolbar/assets/iconHand.svg'
import iconMousePointer from '../editorToolbar/assets/iconMousePointer.svg'
import iconRotateLeft from '../editorToolbar/assets/iconRotateLeft.svg'
import iconRotateRight from '../editorToolbar/assets/iconRotateRight.svg'
import { InspectorPanel } from '../inspectorPanel/InspectorPanel'
import styles from './EditorMobileLayout.module.css'

interface EditorMobileLayoutProps
  extends ComponentProps<typeof InspectorPanel> {
  canEdit: boolean
  isCreatingProjectCopy: boolean
  isSavingVersion: boolean
  isPropertiesOpen: boolean
  onBack: () => void
  onCloseProperties: () => void
  onCreateCopy: () => void
  onExport: () => void
  onOpenProperties: () => void
  onSaveVersion: () => void
  projectName: string
  scene: ReactNode
}

function getFillColor(
  selectedNode: ComponentProps<typeof InspectorPanel>['selectedNode'],
) {
  if (
    !selectedNode ||
    (selectedNode.type !== 'shape' && selectedNode.type !== 'text') ||
    selectedNode.style.fill.type !== 'color'
  ) {
    return null
  }

  return selectedNode.style.fill.value
}

export function EditorMobileLayout({
  canEdit,
  isCreatingProjectCopy,
  isSavingVersion,
  isPropertiesOpen,
  onBack,
  onCloseProperties,
  onCreateCopy,
  onExport,
  onOpenProperties,
  onSaveVersion,
  projectName,
  scene,
  selectedNode,
  ...inspectorProps
}: EditorMobileLayoutProps) {
  const hasSelection = Boolean(selectedNode)
  const [isColorPickerOpen, setIsColorPickerOpen] = useState(false)
  const [isProjectMenuOpen, setIsProjectMenuOpen] = useState(false)
  const fillColor = getFillColor(selectedNode)

  function handleFillChange(color: string) {
    if (selectedNode?.type === 'shape') {
      inspectorProps.onShapeFillChange(selectedNode.id, color)

      return
    }

    if (selectedNode?.type === 'text') {
      inspectorProps.onTextFillColorChange(selectedNode.id, color)
    }
  }

  return (
    <div className={styles.layout}>
      <header className={styles.header}>
        <IconButton className={styles.headerButton} onClick={onBack}>
          <span className={styles.backIcon}>←</span>
        </IconButton>
        <div className={styles.title}>
          <span>Brandkit webview</span>
          <strong>{projectName}</strong>
        </div>
        <div
          className={styles.menuAnchor}
          onPointerDown={(event) => event.stopPropagation()}
        >
          <IconButton
            className={styles.headerButton}
            onClick={() => setIsProjectMenuOpen((isOpen) => !isOpen)}
          >
            <span className={styles.moreIcon}>•••</span>
          </IconButton>
          {isProjectMenuOpen && (
            <PopupMenu
              items={[
                { label: 'Export', onClick: onExport },
                ...(canEdit
                  ? [
                      {
                        label: isSavingVersion ? 'Saving…' : 'Save',
                        onClick: onSaveVersion,
                      },
                    ]
                  : [
                      {
                        label: isCreatingProjectCopy ? 'Opening…' : 'Explore',
                        onClick: onCreateCopy,
                      },
                    ]),
              ]}
              onClose={() => setIsProjectMenuOpen(false)}
            />
          )}
        </div>
      </header>
      <div className={styles.scene}>{scene}</div>
      {canEdit && (
        <div className={styles.selectionActions}>
          {hasSelection ? (
            <>
              <button
                disabled={!fillColor}
                onClick={() => setIsColorPickerOpen(true)}
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
      )}
      {canEdit && (
        <>
          <div className={styles.canvasControls}>
            {fillColor ? (
              <ColorSwatch
                className={styles.fillSwatch}
                color={fillColor}
                onClick={() => setIsColorPickerOpen(true)}
              />
            ) : (
              <IconButton className={styles.activeTool}>
                <img alt="" src={iconMousePointer} />
              </IconButton>
            )}
            <IconButton>
              <img alt="" src={iconHand} />
            </IconButton>
            <span />
            <IconButton>
              <img alt="" src={iconRotateLeft} />
            </IconButton>
            <IconButton>
              <img alt="" src={iconRotateRight} />
            </IconButton>
            <span />
            <IconButton>
              <span className={styles.fitIcon}>⌗</span>
            </IconButton>
          </div>
          <button
            className={styles.propertiesTrigger}
            onClick={onOpenProperties}
            type="button"
          >
            Properties <span>⌃</span>
          </button>
          <BottomSheet
            actions={
              <button
                className={styles.doneButton}
                onClick={onCloseProperties}
                type="button"
              >
                Done
              </button>
            }
            isOpen={isPropertiesOpen}
            onClose={onCloseProperties}
            title="Element properties"
          >
            <InspectorPanel
              {...inspectorProps}
              selectedNode={selectedNode}
              showHeader={false}
            />
          </BottomSheet>
          {fillColor && (
            <BottomSheet
              actions={
                <button
                  className={styles.doneButton}
                  onClick={() => setIsColorPickerOpen(false)}
                  type="button"
                >
                  Done
                </button>
              }
              isOpen={isColorPickerOpen}
              onClose={() => setIsColorPickerOpen(false)}
              title="Fill color"
            >
              <div className={styles.colorPickerContent}>
                <ColorPicker color={fillColor} onChange={handleFillChange} />
              </div>
            </BottomSheet>
          )}
        </>
      )}
    </div>
  )
}
