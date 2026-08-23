import { AppRoute } from '@app/providers/router/routes'
import { type EditorDocument, EditorStore } from '@entities/editorDocument'
import { exportEditorDocument } from '@features/editorExport'
import { VersionHistoryModal } from '@features/editorVersionHistory'
import {
  ConfirmUnsavedChangeModal,
  type ImageExportFileType,
  ImageExportModal,
  useToast,
} from '@shared/ui'
import {
  type CanvasTool,
  EditorScene,
  type ViewportOffset,
} from '@widgets/editorScene'
import { observer } from 'mobx-react-lite'
import { useCallback, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useBlocker, useNavigate } from 'react-router-dom'
import { useEditorKeyboardShortcuts } from '../model/useEditorKeyboardShortcuts'
import { useEditorProjectActions } from '../model/useEditorProjectActions'
import { useIsMobileLayout } from '../model/useIsMobileLayout'
import styles from './EditorWorkspace.module.css'
import { EditorDesktopLayout } from './editorDesktopLayout/EditorDesktopLayout'
import { EditorMobileLayout } from './editorMobileLayout/EditorMobileLayout'
import { EditorStatusBar } from './editorStatusBar/EditorStatusBar'
import { EditorToolbar } from './editorToolbar/EditorToolbar'
import { InspectorPanel } from './inspectorPanel/InspectorPanel'
import { LayersPanel } from './layersPanel/LayersPanel'

interface EditorWorkspaceProps {
  canEdit: boolean
  document: EditorDocument
  projectId: string
  projectName: string
}

const MAX_ZOOM = 2
const MIN_ZOOM = 0.5
const ZOOM_STEP = 0.25
const INITIAL_VIEWPORT_OFFSET: ViewportOffset = { x: 0, y: 0 }

export const EditorWorkspace = observer(function EditorWorkspace({
  canEdit,
  document,
  projectId,
  projectName: initialProjectName,
}: EditorWorkspaceProps) {
  const isMobileLayout = useIsMobileLayout()
  const navigate = useNavigate()
  const toast = useToast()
  const { t } = useTranslation()
  const [editorStore, setEditorStore] = useState(
    () => new EditorStore(document),
  )
  const [sceneRenderVersion, setSceneRenderVersion] = useState(0)
  const [activeTool, setActiveTool] = useState<CanvasTool>(
    isMobileLayout ? 'hand' : 'select',
  )
  const [zoom, setZoom] = useState(1)
  const [viewportOffset, setViewportOffset] = useState(INITIAL_VIEWPORT_OFFSET)
  const [isMobileInspectorOpen, setIsMobileInspectorOpen] = useState(false)
  const [isImageExportModalOpen, setIsImageExportModalOpen] = useState(false)
  const [isImageExporting, setIsImageExporting] = useState(false)
  const [isVersionHistoryOpen, setIsVersionHistoryOpen] = useState(false)
  const [measuredRotationCenter, setMeasuredRotationCenter] = useState<{
    nodeId: string
    x: number
    y: number
  } | null>(null)
  const {
    createCopy,
    hasUnsavedChanges,
    isCreatingCopy,
    isSavingVersion,
    markDocumentAsSaved,
    projectName,
    renameProject,
    saveVersion,
    shouldBlockNavigation,
  } = useEditorProjectActions({
    canEdit,
    document: editorStore.document,
    initialProjectName,
    projectId,
  })
  const navigationBlocker = useBlocker(shouldBlockNavigation)

  const handleUndo = useCallback(() => {
    const wasUndone = editorStore.undo()

    if (wasUndone) {
      setSceneRenderVersion((version) => version + 1)
    }

    return wasUndone
  }, [editorStore])

  const handleRedo = useCallback(() => {
    const wasRedone = editorStore.redo()

    if (wasRedone) {
      setSceneRenderVersion((version) => version + 1)
    }

    return wasRedone
  }, [editorStore])

  function handleZoomIn() {
    setZoom((currentZoom) => Math.min(currentZoom + ZOOM_STEP, MAX_ZOOM))
  }

  function handleZoomOut() {
    setZoom((currentZoom) => Math.max(currentZoom - ZOOM_STEP, MIN_ZOOM))
  }

  function constrainZoom(nextZoom: number) {
    return Math.min(Math.max(nextZoom, MIN_ZOOM), MAX_ZOOM)
  }

  function handleZoomChange(nextZoom: number) {
    const constrainedZoom = constrainZoom(nextZoom)

    setZoom(constrainedZoom)

    return constrainedZoom
  }

  function handleExport() {
    setIsImageExportModalOpen(true)
  }

  function handleOpenMobileProperties() {
    editorStore.beginHistoryTransaction()
    setIsMobileInspectorOpen(true)
  }

  function handleOpenMobileColorPicker() {
    editorStore.beginHistoryTransaction()
  }

  function handleDoneMobileProperties() {
    editorStore.commitHistoryTransaction()
    setIsMobileInspectorOpen(false)
  }

  function handleCancelMobileProperties() {
    const wasCancelled = editorStore.cancelHistoryTransaction()

    if (wasCancelled) {
      setSceneRenderVersion((version) => version + 1)
    }

    setIsMobileInspectorOpen(false)
  }

  function handleRestoreDocument(restoredDocument: EditorDocument) {
    setEditorStore(new EditorStore(restoredDocument))
    markDocumentAsSaved(restoredDocument)
    setSceneRenderVersion((version) => version + 1)
    setIsMobileInspectorOpen(false)
  }

  async function handleImageExport(fileType: ImageExportFileType) {
    setIsImageExporting(true)

    try {
      await exportEditorDocument({
        document: editorStore.document,
        fileName: projectName,
        format: fileType,
      })
      setIsImageExportModalOpen(false)
      toast.info(t('Image exported.'))
    } catch {
      toast.error(t('Unable to export image. Please try again.'))
    } finally {
      setIsImageExporting(false)
    }
  }

  function handleToggleVisibility(nodeId: string) {
    editorStore.svgNodeEditing.toggleNodeVisibility(nodeId)
    setSceneRenderVersion((version) => version + 1)
  }

  function handleSceneSelectNode(nodeId: string, isMultiSelect: boolean) {
    editorStore.selectNode(nodeId, isMultiSelect)
  }

  const handleMeasuredRotationCenterChange = useCallback(
    (nextCenter: { nodeId: string; x: number; y: number } | null) => {
      setMeasuredRotationCenter((currentCenter) =>
        currentCenter?.nodeId === nextCenter?.nodeId &&
        currentCenter?.x === nextCenter?.x &&
        currentCenter?.y === nextCenter?.y
          ? currentCenter
          : nextCenter,
      )
    },
    [],
  )

  const handleDeleteSelection = useCallback(() => {
    if (!editorStore.deleteSelectedNode()) {
      return false
    }

    setIsMobileInspectorOpen(false)
    setSceneRenderVersion((version) => version + 1)

    return true
  }, [editorStore])

  useEditorKeyboardShortcuts({
    canEdit,
    onDeleteSelectedNode: handleDeleteSelection,
    onMoveEnd: editorStore.commitHistoryTransaction,
    onMoveStart: editorStore.beginHistoryTransaction,
    onMoveSelectedNode: editorStore.moveSelectedNode,
    onRedo: handleRedo,
    onSaveVersion: saveVersion,
    onUndo: handleUndo,
  })

  function handleCancelExit() {
    if (navigationBlocker.state === 'blocked') {
      navigationBlocker.reset()
    }
  }

  function handleConfirmExit() {
    if (navigationBlocker.state === 'blocked') {
      navigationBlocker.proceed()
    }
  }

  async function handleSaveAndExit() {
    const wasSaved = await saveVersion()

    if (wasSaved && navigationBlocker.state === 'blocked') {
      navigationBlocker.proceed()
    }
  }

  const inspectorProps = {
    definitions: editorStore.document.definitions,
    measuredRotationCenter:
      measuredRotationCenter &&
      measuredRotationCenter.nodeId === editorStore.selectedNode?.id
        ? { x: measuredRotationCenter.x, y: measuredRotationCenter.y }
        : undefined,
    onDeleteSelectedNode: handleDeleteSelection,
    onGradientStopColorChange: editorStore.svgNodeEditing.setGradientStopColor,
    onShapeFillChange: editorStore.svgNodeEditing.setShapeFillColor,
    onShapeFillOpacityChange: editorStore.svgNodeEditing.setShapeFillOpacity,
    onShapeOpacityChange: editorStore.svgNodeEditing.setShapeOpacity,
    onShapePositionChange: editorStore.svgNodeEditing.setShapePosition,
    onShapeRotationChange: editorStore.svgNodeEditing.setShapeRotation,
    onShapeStrokeAdd: editorStore.svgNodeEditing.addShapeStroke,
    onShapeStrokeColorChange: editorStore.svgNodeEditing.setShapeStrokeColor,
    onShapeStrokeRemove: editorStore.svgNodeEditing.removeShapeStroke,
    onShapeStrokeOpacityChange:
      editorStore.svgNodeEditing.setShapeStrokeOpacity,
    onShapeStrokeWidthChange: editorStore.svgNodeEditing.setShapeStrokeWidth,
    onShapeTransformChange: editorStore.svgNodeEditing.setShapeTransform,
    onTextAnchorChange: editorStore.svgNodeEditing.setTextAnchor,
    onTextContentChange: editorStore.svgNodeEditing.setTextContent,
    onTextFillColorChange: editorStore.svgNodeEditing.setTextFillColor,
    onTextFontFamilyChange: editorStore.svgNodeEditing.setTextFontFamily,
    onTextFontSizeChange: editorStore.svgNodeEditing.setTextFontSize,
    onTextFontStyleChange: editorStore.svgNodeEditing.setTextFontStyle,
    onTextFontWeightChange: editorStore.svgNodeEditing.setTextFontWeight,
    onTextOpacityChange: editorStore.svgNodeEditing.setTextOpacity,
    onTextPositionChange: editorStore.svgNodeEditing.setTextPosition,
    onTextRotationChange: editorStore.svgNodeEditing.setTextRotation,
    onTextTransformChange: editorStore.svgNodeEditing.setTextTransform,
    selectedNode: editorStore.selectedNode,
  }
  const scene = (
    <EditorScene
      activeTool={activeTool}
      constrainZoom={constrainZoom}
      document={editorStore.document}
      isReadOnly={!canEdit}
      onClearSelection={() => editorStore.setSelectedNode(null)}
      onDocumentChangeEnd={editorStore.commitHistoryTransaction}
      onDocumentChangeStart={editorStore.beginHistoryTransaction}
      onGroupPositionChange={editorStore.svgNodeEditing.setGroupPosition}
      onRawPositionChange={editorStore.svgNodeEditing.setRawPosition}
      onMeasuredRotationCenterChange={handleMeasuredRotationCenterChange}
      onSelectNode={handleSceneSelectNode}
      onShapePositionChange={editorStore.svgNodeEditing.setShapePosition}
      onShapeRotationChange={editorStore.svgNodeEditing.setShapeRotation}
      onShapeTransformChange={editorStore.svgNodeEditing.setShapeTransform}
      onTextPositionChange={editorStore.svgNodeEditing.setTextPosition}
      onTextRotationChange={editorStore.svgNodeEditing.setTextRotation}
      onTextTransformChange={editorStore.svgNodeEditing.setTextTransform}
      onViewportOffsetChange={setViewportOffset}
      onZoomChange={handleZoomChange}
      renderVersion={sceneRenderVersion}
      selectedNode={editorStore.selectedNode}
      selectedNodes={editorStore.selectedNodes}
      viewportOffset={viewportOffset}
      zoom={zoom}
    />
  )

  return (
    <section className={styles.workspace}>
      {isMobileLayout ? (
        <EditorMobileLayout
          {...inspectorProps}
          activeTool={activeTool}
          canEdit={canEdit}
          canRedo={editorStore.canRedo}
          canUndo={editorStore.canUndo}
          isCreatingProjectCopy={isCreatingCopy}
          isSavingVersion={isSavingVersion}
          isPropertiesOpen={isMobileInspectorOpen}
          onBack={() => navigate(-1)}
          onCancelColorPicker={handleCancelMobileProperties}
          onCancelProperties={handleCancelMobileProperties}
          onCreateCopy={createCopy}
          onDeleteSelection={handleDeleteSelection}
          onExport={handleExport}
          onDoneColorPicker={handleDoneMobileProperties}
          onOpenColorPicker={handleOpenMobileColorPicker}
          onOpenProperties={handleOpenMobileProperties}
          onOpenVersionHistory={() => setIsVersionHistoryOpen(true)}
          onRedo={handleRedo}
          onSaveVersion={saveVersion}
          onDoneProperties={handleDoneMobileProperties}
          onToolChange={setActiveTool}
          onUndo={handleUndo}
          onZoomIn={handleZoomIn}
          onZoomOut={handleZoomOut}
          projectName={projectName}
          scene={scene}
          zoom={zoom}
        />
      ) : (
        <EditorDesktopLayout
          canEdit={canEdit}
          inspector={<InspectorPanel {...inspectorProps} />}
          layers={
            <LayersPanel
              expandedGroupIds={editorStore.expandedGroupIds}
              nodes={editorStore.document.layers}
              onSelectNode={editorStore.selectNode}
              onToggleGroup={editorStore.toggleGroup}
              onToggleVisibility={handleToggleVisibility}
              selectedNodeIds={editorStore.selectedNodeIds}
            />
          }
          scene={scene}
          status={
            <EditorStatusBar
              selectedNode={editorStore.selectedNode}
              zoom={zoom}
            />
          }
          toolbar={
            <EditorToolbar
              activeTool={activeTool}
              canEdit={canEdit}
              canRedo={editorStore.canRedo}
              canUndo={editorStore.canUndo}
              isCreatingProjectCopy={isCreatingCopy}
              isSavingVersion={isSavingVersion}
              onBack={() => navigate(-1)}
              onCreateCopy={createCopy}
              onEditProjectName={renameProject}
              onExport={handleExport}
              onOpenProfile={() =>
                navigate(AppRoute.Profile, { viewTransition: true })
              }
              onOpenVersionHistory={() => setIsVersionHistoryOpen(true)}
              onToolChange={setActiveTool}
              onRedo={handleRedo}
              onSaveVersion={saveVersion}
              onUndo={handleUndo}
              onZoomIn={handleZoomIn}
              onZoomOut={handleZoomOut}
              projectName={projectName}
              zoom={zoom}
            />
          }
        />
      )}
      <ConfirmUnsavedChangeModal
        isOpen={navigationBlocker.state === 'blocked'}
        isSaving={isSavingVersion}
        onDismiss={handleCancelExit}
        onLeave={handleConfirmExit}
        onSave={handleSaveAndExit}
      />
      <ImageExportModal
        isExporting={isImageExporting}
        isOpen={isImageExportModalOpen}
        onClose={() => setIsImageExportModalOpen(false)}
        onExport={handleImageExport}
      />
      {canEdit && (
        <VersionHistoryModal
          hasUnsavedChanges={hasUnsavedChanges}
          isOpen={isVersionHistoryOpen}
          onClose={() => setIsVersionHistoryOpen(false)}
          onRestoreDocument={handleRestoreDocument}
          projectId={projectId}
        />
      )}
    </section>
  )
})
