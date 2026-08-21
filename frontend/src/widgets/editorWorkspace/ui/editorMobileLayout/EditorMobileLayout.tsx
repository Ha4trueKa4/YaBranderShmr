import { BottomSheet, ColorPicker } from '@shared/ui'
import type { CanvasTool } from '@widgets/editorScene'
import type { ComponentProps, ReactNode } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'

import { InspectorPanel } from '../inspectorPanel/InspectorPanel'
import { MobileCanvasControls } from './components/MobileCanvasControls'
import { MobileEditorHeader } from './components/MobileEditorHeader'
import { MobileSelectionActions } from './components/MobileSelectionActions'
import styles from './EditorMobileLayout.module.css'

interface EditorMobileLayoutProps
  extends ComponentProps<typeof InspectorPanel> {
  activeTool: CanvasTool
  canEdit: boolean
  isCreatingProjectCopy: boolean
  isSavingVersion: boolean
  isPropertiesOpen: boolean
  onBack: () => void
  onCloseProperties: () => void
  onCreateCopy: () => void
  onDeleteSelection: () => void
  onExport: () => void
  onOpenProperties: () => void
  onOpenVersionHistory: () => void
  onSaveVersion: () => void
  onToolChange: (tool: CanvasTool) => void
  onZoomIn: () => void
  onZoomOut: () => void
  onZoomReset: () => void
  projectName: string
  scene: ReactNode
  zoom: number
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
  activeTool,
  canEdit,
  isCreatingProjectCopy,
  isSavingVersion,
  isPropertiesOpen,
  onBack,
  onCloseProperties,
  onCreateCopy,
  onDeleteSelection,
  onExport,
  onOpenProperties,
  onOpenVersionHistory,
  onSaveVersion,
  onToolChange,
  onZoomIn,
  onZoomOut,
  onZoomReset,
  projectName,
  scene,
  selectedNode,
  zoom,
  ...inspectorProps
}: EditorMobileLayoutProps) {
  const { t } = useTranslation()
  const hasSelection = Boolean(selectedNode)
  const canDeleteSelection = Boolean(selectedNode && !selectedNode.isLocked)
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
        onOpenVersionHistory={onOpenVersionHistory}
        onSaveVersion={onSaveVersion}
        projectName={projectName}
      />
      <div className={styles.scene}>{scene}</div>
      {canEdit && (
        <MobileSelectionActions
          canDeleteSelection={canDeleteSelection}
          fillColor={fillColor}
          hasSelection={hasSelection}
          onDeleteSelection={onDeleteSelection}
          onOpenColorPicker={() => setIsColorPickerOpen(true)}
        />
      )}
      {canEdit && (
        <>
          <MobileCanvasControls
            activeTool={activeTool}
            fillColor={fillColor}
            onOpenColorPicker={() => setIsColorPickerOpen(true)}
            onToolChange={onToolChange}
            onZoomIn={onZoomIn}
            onZoomOut={onZoomOut}
            onZoomReset={onZoomReset}
            zoom={zoom}
          />
          <button
            className={styles.propertiesTrigger}
            onClick={onOpenProperties}
            type="button"
          >
            {t('Properties')} <span>⌃</span>
          </button>
          <BottomSheet
            actions={
              <button
                className={styles.doneButton}
                onClick={onCloseProperties}
                type="button"
              >
                {t('Done')}
              </button>
            }
            isOpen={isPropertiesOpen}
            onClose={onCloseProperties}
            title={t('Element properties')}
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
                  {t('Done')}
                </button>
              }
              isOpen={isColorPickerOpen}
              onClose={() => setIsColorPickerOpen(false)}
              title={t('Fill color')}
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
