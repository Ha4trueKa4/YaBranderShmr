import { AppRoute } from '@app/providers/router/routes'
import {
  type EditorDocument,
  EditorStore,
  serializeSvg,
} from '@entities/editorDocument'
import {
  projectQueryKey,
  projectSvgQueryKey,
  useUpdateProjectMetadataMutation,
} from '@entities/project'
import { useCreateProjectMutation } from '@features/projectCreate'
import { getApiErrorMessage } from '@shared/api/api'
import {
  ConfirmUnsavedChangeModal,
  type ImageExportFileType,
  ImageExportModal,
  useToast,
} from '@shared/ui'
import { useQueryClient } from '@tanstack/react-query'
import { EditorScene } from '@widgets/editorScene'
import { observer } from 'mobx-react-lite'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useBlocker, useNavigate } from 'react-router-dom'
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

const mobileLayoutMediaQuery = '(width < 640px)'

function isEditableElement(target: EventTarget | null) {
  return (
    target instanceof HTMLInputElement ||
    target instanceof HTMLSelectElement ||
    target instanceof HTMLTextAreaElement ||
    (target instanceof HTMLElement && target.isContentEditable)
  )
}

function useIsMobileLayout() {
  const [isMobileLayout, setIsMobileLayout] = useState(
    () =>
      typeof window !== 'undefined' &&
      window.matchMedia(mobileLayoutMediaQuery).matches,
  )

  useEffect(() => {
    const mediaQuery = window.matchMedia(mobileLayoutMediaQuery)
    const handleChange = () => setIsMobileLayout(mediaQuery.matches)

    handleChange()
    mediaQuery.addEventListener('change', handleChange)

    return () => mediaQuery.removeEventListener('change', handleChange)
  }, [])

  return isMobileLayout
}

export const EditorWorkspace = observer(function EditorWorkspace({
  canEdit,
  document,
  projectId,
  projectName: initialProjectName,
}: EditorWorkspaceProps) {
  const isMobileLayout = useIsMobileLayout()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const toast = useToast()
  const [projectName, setProjectName] = useState(initialProjectName)
  const [editorStore] = useState(() => new EditorStore(document))
  const [savedProjectSvg, setSavedProjectSvg] = useState(() =>
    serializeSvg(document),
  )
  const [sceneRenderVersion, setSceneRenderVersion] = useState(0)
  const [isMobileInspectorOpen, setIsMobileInspectorOpen] = useState(false)
  const [isImageExportModalOpen, setIsImageExportModalOpen] = useState(false)
  const [isCopyingProject, setIsCopyingProject] = useState(false)
  const isLeavingAfterCopyRef = useRef(false)
  const { isPending: isSavingProjectVersion, mutateAsync: saveProjectVersion } =
    useUpdateProjectMetadataMutation()
  const { isPending: isCreatingProjectCopy, mutateAsync: createProject } =
    useCreateProjectMutation()
  const hasUnsavedChanges =
    serializeSvg(editorStore.document) !== savedProjectSvg
  const shouldBlockNavigation = useCallback(
    () => canEdit && hasUnsavedChanges && !isLeavingAfterCopyRef.current,
    [canEdit, hasUnsavedChanges],
  )
  const navigationBlocker = useBlocker(shouldBlockNavigation)

  function handleToolbarAction() {
    // TODO: подключить действия после реализации editorStore.
  }

  function handleExport() {
    setIsImageExportModalOpen(true)
  }

  function handleImageExport(fileType: ImageExportFileType) {
    setIsImageExportModalOpen(false)
    toast.info(`${fileType.toUpperCase()} export is not available yet.`)
  }

  async function handleProjectNameSubmit(nextProjectName: string) {
    if (nextProjectName === projectName) {
      return
    }

    try {
      const updatedProject = await saveProjectVersion({
        name: nextProjectName,
        projectId,
      })

      setProjectName(updatedProject.name ?? nextProjectName)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: projectQueryKey(projectId) }),
        queryClient.invalidateQueries({ queryKey: ['user-profile'] }),
        queryClient.invalidateQueries({ queryKey: ['feed'] }),
      ])
      toast.info('Project name updated.')
    } catch (error) {
      toast.error(getApiErrorMessage(error))

      throw error
    }
  }

  async function handleSaveVersion() {
    const projectSvg = serializeSvg(editorStore.document)

    if (projectSvg === savedProjectSvg) {
      toast.error('There are no changes to save.')

      return
    }

    try {
      await saveProjectVersion({
        data: projectSvg,
        projectId,
      })
      setSavedProjectSvg(projectSvg)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: projectQueryKey(projectId) }),
        queryClient.invalidateQueries({
          queryKey: projectSvgQueryKey(projectId),
        }),
      ])
      toast.info('Project version saved.')
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    }
  }

  async function handleCreateCopy() {
    setIsCopyingProject(true)

    try {
      const createdProject = await createProject({
        name: projectName,
      })

      if (!createdProject.id) {
        throw new TypeError('Created project ID is missing')
      }

      await saveProjectVersion({
        data: serializeSvg(editorStore.document),
        projectId: createdProject.id,
      })
      isLeavingAfterCopyRef.current = true
      navigate(`/projects/${createdProject.id}`, { viewTransition: true })
      toast.info('Project copy created.')
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsCopyingProject(false)
    }
  }

  function handleToggleVisibility(nodeId: string) {
    editorStore.toggleNodeVisibility(nodeId)
    setSceneRenderVersion((version) => version + 1)
  }

  function handleSceneSelectNode(nodeId: string) {
    editorStore.setSelectedNode(nodeId)
  }

  const handleDeleteSelection = useCallback(() => {
    if (!editorStore.deleteSelectedNode()) {
      return false
    }

    setIsMobileInspectorOpen(false)
    setSceneRenderVersion((version) => version + 1)

    return true
  }, [editorStore])

  useEffect(() => {
    if (!canEdit) {
      return
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (
        event.key !== 'Backspace' ||
        event.defaultPrevented ||
        event.metaKey ||
        event.ctrlKey ||
        event.altKey ||
        isEditableElement(event.target)
      ) {
        return
      }

      if (handleDeleteSelection()) {
        event.preventDefault()
      }
    }

    window.addEventListener('keydown', handleKeyDown)

    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [canEdit, handleDeleteSelection])

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
    onShapeFillChange: editorStore.setShapeFillColor,
    onShapeFillOpacityChange: editorStore.setShapeFillOpacity,
    onShapeOpacityChange: editorStore.setShapeOpacity,
    onShapePositionChange: editorStore.setShapePosition,
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
      document={editorStore.document}
      isReadOnly={!canEdit}
      onSelectNode={handleSceneSelectNode}
      onShapePositionChange={editorStore.setShapePosition}
      onTextPositionChange={editorStore.setTextPosition}
      renderVersion={sceneRenderVersion}
      selectedNode={editorStore.selectedNode}
    />
  )

  return (
    <section className={styles.workspace}>
      {isMobileLayout ? (
        <EditorMobileLayout
          {...inspectorProps}
          canEdit={canEdit}
          isCreatingProjectCopy={isCopyingProject || isCreatingProjectCopy}
          isSavingVersion={isSavingProjectVersion}
          isPropertiesOpen={isMobileInspectorOpen}
          onBack={() => navigate(-1)}
          onCloseProperties={() => setIsMobileInspectorOpen(false)}
          onCreateCopy={handleCreateCopy}
          onDeleteSelection={handleDeleteSelection}
          onExport={handleExport}
          onOpenProperties={() => setIsMobileInspectorOpen(true)}
          onSaveVersion={handleSaveVersion}
          projectName={projectName}
          scene={scene}
        />
      ) : (
        <EditorDesktopLayout
          canEdit={canEdit}
          inspector={<InspectorPanel {...inspectorProps} />}
          layers={
            <LayersPanel
              expandedGroupIds={editorStore.expandedGroupIds}
              nodes={editorStore.document.layers}
              onSelectNode={editorStore.setSelectedNode}
              onToggleGroup={editorStore.toggleGroup}
              onToggleVisibility={handleToggleVisibility}
              selectedNodeId={editorStore.selectedNodeId}
            />
          }
          scene={scene}
          status={<EditorStatusBar />}
          toolbar={
            <EditorToolbar
              canEdit={canEdit}
              isCreatingProjectCopy={isCopyingProject || isCreatingProjectCopy}
              isSavingVersion={isSavingProjectVersion}
              onCreateCopy={handleCreateCopy}
              onEditProjectName={handleProjectNameSubmit}
              onExport={handleExport}
              onOpenProfile={() =>
                navigate(AppRoute.Profile, { viewTransition: true })
              }
              onPanTool={handleToolbarAction}
              onRedo={handleToolbarAction}
              onSaveVersion={handleSaveVersion}
              onSelectTool={handleToolbarAction}
              onUndo={handleToolbarAction}
              onZoomIn={handleToolbarAction}
              onZoomOut={handleToolbarAction}
              projectName={projectName}
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
        isOpen={isImageExportModalOpen}
        onClose={() => setIsImageExportModalOpen(false)}
        onExport={handleImageExport}
      />
    </section>
  )
})
