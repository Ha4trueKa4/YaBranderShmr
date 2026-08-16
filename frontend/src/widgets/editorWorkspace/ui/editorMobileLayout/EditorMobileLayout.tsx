import { BottomSheet, ColorPicker } from '@shared/ui'
import type { ComponentProps, ReactNode } from 'react'
import { useState } from 'react'

import { InspectorPanel } from '../inspectorPanel/InspectorPanel'
import { MobileCanvasControls } from './components/MobileCanvasControls'
import { MobileEditorHeader } from './components/MobileEditorHeader'
import { MobileSelectionActions } from './components/MobileSelectionActions'
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
      <MobileEditorHeader
        canEdit={canEdit}
        isCreatingProjectCopy={isCreatingProjectCopy}
        isSavingVersion={isSavingVersion}
        onBack={onBack}
        onCreateCopy={onCreateCopy}
        onExport={onExport}
        onSaveVersion={onSaveVersion}
        projectName={projectName}
      />
      <div className={styles.scene}>{scene}</div>
      {canEdit && (
        <MobileSelectionActions
          fillColor={fillColor}
          hasSelection={hasSelection}
          onOpenColorPicker={() => setIsColorPickerOpen(true)}
        />
      )}
      {canEdit && (
        <>
          <MobileCanvasControls
            fillColor={fillColor}
            onOpenColorPicker={() => setIsColorPickerOpen(true)}
          />
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
