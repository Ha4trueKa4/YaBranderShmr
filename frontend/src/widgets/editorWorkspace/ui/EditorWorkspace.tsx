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
import { useToast } from '@shared/ui'
import { useQueryClient } from '@tanstack/react-query'
import { EditorScene } from '@widgets/editorScene'
import { observer } from 'mobx-react-lite'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
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

export const EditorWorkspace = observer(function EditorWorkspace({
  canEdit,
  document,
  projectId,
  projectName: initialProjectName,
}: EditorWorkspaceProps) {
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
  const [isCopyingProject, setIsCopyingProject] = useState(false)
  const { isPending: isSavingProjectVersion, mutateAsync: saveProjectVersion } =
    useUpdateProjectMetadataMutation()
  const { isPending: isCreatingProjectCopy, mutateAsync: createProject } =
    useCreateProjectMutation()

  function handleToolbarAction() {
    // TODO: подключить действия после реализации editorStore.
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

  return (
    <section className={styles.workspace}>
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
        scene={
          <EditorScene
            document={editorStore.document}
            isReadOnly={!canEdit}
            onSelectNode={handleSceneSelectNode}
            onShapePositionChange={editorStore.setShapePosition}
            onTextPositionChange={editorStore.setTextPosition}
            renderVersion={sceneRenderVersion}
            selectedNode={editorStore.selectedNode}
          />
        }
        status={<EditorStatusBar />}
        toolbar={
          <EditorToolbar
            canEdit={canEdit}
            isCreatingProjectCopy={isCopyingProject || isCreatingProjectCopy}
            isSavingVersion={isSavingProjectVersion}
            onCreateCopy={handleCreateCopy}
            onEditProjectName={handleProjectNameSubmit}
            onExport={handleToolbarAction}
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
      <EditorMobileLayout
        {...inspectorProps}
        canEdit={canEdit}
        isCreatingProjectCopy={isCopyingProject || isCreatingProjectCopy}
        isSavingVersion={isSavingProjectVersion}
        isPropertiesOpen={isMobileInspectorOpen}
        onBack={() => navigate(-1)}
        onCloseProperties={() => setIsMobileInspectorOpen(false)}
        onCreateCopy={handleCreateCopy}
        onExport={handleToolbarAction}
        onOpenProperties={() => setIsMobileInspectorOpen(true)}
        onSaveVersion={handleSaveVersion}
        projectName={projectName}
        scene={
          <EditorScene
            document={editorStore.document}
            isReadOnly={!canEdit}
            onSelectNode={handleSceneSelectNode}
            onShapePositionChange={editorStore.setShapePosition}
            onTextPositionChange={editorStore.setTextPosition}
            renderVersion={sceneRenderVersion}
            selectedNode={editorStore.selectedNode}
          />
        }
      />
    </section>
  )
})
