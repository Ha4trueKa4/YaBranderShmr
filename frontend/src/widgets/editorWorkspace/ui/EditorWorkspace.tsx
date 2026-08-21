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
  const [activeTool, setActiveTool] = useState<CanvasTool>('select')
  const [zoom, setZoom] = useState(1)
  const [viewportOffset, setViewportOffset] = useState(INITIAL_VIEWPORT_OFFSET)
  const [isMobileInspectorOpen, setIsMobileInspectorOpen] = useState(false)
  const [isImageExportModalOpen, setIsImageExportModalOpen] = useState(false)
  const [isImageExporting, setIsImageExporting] = useState(false)
  const [isVersionHistoryOpen, setIsVersionHistoryOpen] = useState(false)
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

  function handleToolbarAction() {
    // TODO: подключить действия после реализации editorStore.
  }

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
    editorStore.toggleNodeVisibility(nodeId)
    setSceneRenderVersion((version) => version + 1)
  }

  function handleSceneSelectNode(nodeId: string, isMultiSelect: boolean) {
    editorStore.selectNode(nodeId, isMultiSelect)
  }

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
    onMoveSelectedNode: editorStore.moveSelectedNode,
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

  const inspectorProps = {
    definitions: editorStore.document.definitions,
    onGradientStopColorChange: editorStore.setGradientStopColor,
    onShapeFillChange: editorStore.setShapeFillColor,
    onShapeFillOpacityChange: editorStore.setShapeFillOpacity,
    onShapeOpacityChange: editorStore.setShapeOpacity,
    onShapePositionChange: editorStore.setShapePosition,
    onShapeStrokeAdd: editorStore.addShapeStroke,
    onShapeStrokeColorChange: editorStore.setShapeStrokeColor,
    onShapeStrokeRemove: editorStore.removeShapeStroke,
    onShapeStrokeWidthChange: editorStore.setShapeStrokeWidth,
    onTextAnchorChange: editorStore.setTextAnchor,
    onTextContentChange: editorStore.setTextContent,
    onTextFillColorChange: editorStore.setTextFillColor,
    onTextFontFamilyChange: editorStore.setTextFontFamily,
    onTextFontSizeChange: editorStore.setTextFontSize,
    onTextFontStyleChange: editorStore.setTextFontStyle,
    onTextFontWeightChange: editorStore.setTextFontWeight,
    onTextPositionChange: editorStore.setTextPosition,
    selectedNode: editorStore.selectedNode,
  }
  const scene = (
    <EditorScene
      activeTool={activeTool}
      constrainZoom={constrainZoom}
      document={editorStore.document}
      isReadOnly={!canEdit}
      onClearSelection={() => editorStore.setSelectedNode(null)}
      onGroupPositionChange={editorStore.setGroupPosition}
      onSelectNode={handleSceneSelectNode}
      onShapePositionChange={editorStore.setShapePosition}
      onTextPositionChange={editorStore.setTextPosition}
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
          isCreatingProjectCopy={isCreatingCopy}
          isSavingVersion={isSavingVersion}
          isPropertiesOpen={isMobileInspectorOpen}
          onBack={() => navigate(-1)}
          onCloseProperties={() => setIsMobileInspectorOpen(false)}
          onCreateCopy={createCopy}
          onDeleteSelection={handleDeleteSelection}
          onExport={handleExport}
          onOpenProperties={() => setIsMobileInspectorOpen(true)}
          onOpenVersionHistory={() => setIsVersionHistoryOpen(true)}
          onSaveVersion={saveVersion}
          onToolChange={setActiveTool}
          onZoomIn={handleZoomIn}
          onZoomOut={handleZoomOut}
          onZoomReset={() => setZoom(1)}
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
          status={<EditorStatusBar />}
          toolbar={
            <EditorToolbar
              activeTool={activeTool}
              canEdit={canEdit}
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
              onRedo={handleToolbarAction}
              onSaveVersion={saveVersion}
              onUndo={handleToolbarAction}
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
        onCancel={handleCancelExit}
        onConfirm={handleConfirmExit}
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
