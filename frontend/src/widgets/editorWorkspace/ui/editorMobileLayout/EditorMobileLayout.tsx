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
  onCancelColorPicker: () => void
  onCancelProperties: () => void
  onCreateCopy: () => void
  onDeleteSelection: () => void
  onExport: () => void
  onOpenProperties: () => void
  onOpenVersionHistory: () => void
  onOpenColorPicker: () => void
  onDoneColorPicker: () => void
  onSaveVersion: () => void
  onDoneProperties: () => void
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
  onCancelColorPicker,
  onCancelProperties,
  onCreateCopy,
  onDeleteSelection,
  onExport,
  onOpenProperties,
  onOpenVersionHistory,
  onOpenColorPicker,
  onDoneColorPicker,
  onSaveVersion,
  onDoneProperties,
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
  const [isPropertiesSheetHidden, setIsPropertiesSheetHidden] = useState(false)
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

  function handleOpenColorPicker() {
    onOpenColorPicker()
    setIsColorPickerOpen(true)
  }

  function handleDoneColorPicker() {
    onDoneColorPicker()
    setIsColorPickerOpen(false)
  }

  function handleCancelColorPicker() {
    onCancelColorPicker()
    setIsColorPickerOpen(false)
  }

  function handleEyeDropperStart() {
    setIsColorPickerOpen(false)
    setIsPropertiesSheetHidden(true)
  }

  function handleEyeDropperEnd() {
    setIsColorPickerOpen(true)
    setIsPropertiesSheetHidden(false)
  }

  function handlePropertiesEyeDropperStart() {
    setIsPropertiesSheetHidden(true)
  }

  function handlePropertiesEyeDropperEnd() {
    setIsPropertiesSheetHidden(false)
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
          onOpenColorPicker={handleOpenColorPicker}
        />
      )}
      {canEdit && (
        <>
          <MobileCanvasControls
            activeTool={activeTool}
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
              <>
                <button
                  className={styles.cancelButton}
                  onClick={onCancelProperties}
                  type="button"
                >
                  {t('Cancel')}
                </button>
                <button
                  className={styles.doneButton}
                  onClick={onDoneProperties}
                  type="button"
                >
                  {t('Done')}
                </button>
              </>
            }
            isOpen={isPropertiesOpen && !isPropertiesSheetHidden}
            onClose={onCancelProperties}
            showCloseButton={false}
            title={t('Element properties')}
          >
            <InspectorPanel
              {...inspectorProps}
              onEyeDropperEnd={handlePropertiesEyeDropperEnd}
              onEyeDropperStart={handlePropertiesEyeDropperStart}
              selectedNode={selectedNode}
              showHeader={false}
            />
          </BottomSheet>
          {fillColor && (
            <BottomSheet
              actions={
                <>
                  <button
                    className={styles.cancelButton}
                    onClick={handleCancelColorPicker}
                    type="button"
                  >
                    {t('Cancel')}
                  </button>
                  <button
                    className={styles.doneButton}
                    onClick={handleDoneColorPicker}
                    type="button"
                  >
                    {t('Done')}
                  </button>
                </>
              }
              isOpen={isColorPickerOpen}
              onClose={handleCancelColorPicker}
              showCloseButton={false}
              title={t('Fill color')}
            >
              <div className={styles.colorPickerContent}>
                <ColorPicker
                  color={fillColor}
                  onChange={handleFillChange}
                  onEyeDropperEnd={handleEyeDropperEnd}
                  onEyeDropperStart={handleEyeDropperStart}
                />
              </div>
            </BottomSheet>
          )}
        </>
      )}
    </div>
  )
}
