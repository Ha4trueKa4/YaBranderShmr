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
import { getApiErrorMessage } from '@shared/api/api'
import { useToast } from '@shared/ui'
import { useQueryClient } from '@tanstack/react-query'
import { EditorScene } from '@widgets/editorScene'
import { observer } from 'mobx-react-lite'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styles from './EditorWorkspace.module.css'
import { EditorStatusBar } from './editorStatusBar/EditorStatusBar'
import { EditorToolbar } from './editorToolbar/EditorToolbar'
import { InspectorPanel } from './inspectorPanel/InspectorPanel'
import { LayersPanel } from './layersPanel/LayersPanel'
import { MobileInspector } from './mobileInspector'

interface EditorWorkspaceProps {
  document: EditorDocument
  projectId: string
  projectName: string
}

export const EditorWorkspace = observer(function EditorWorkspace({
  document,
  projectId,
  projectName: initialProjectName,
}: EditorWorkspaceProps) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const toast = useToast()
  const [projectName, setProjectName] = useState(initialProjectName)
  const [editorStore] = useState(() => new EditorStore(document))
  const [sceneRenderVersion, setSceneRenderVersion] = useState(0)
  const [isMobileInspectorOpen, setIsMobileInspectorOpen] = useState(false)
  const { isPending: isSavingProjectVersion, mutateAsync: saveProjectVersion } =
    useUpdateProjectMetadataMutation()

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
    try {
      await saveProjectVersion({
        data: serializeSvg(editorStore.document),
        projectId,
      })
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

  function handleToggleVisibility(nodeId: string) {
    editorStore.toggleNodeVisibility(nodeId)
    setSceneRenderVersion((version) => version + 1)
  }

  function handleSceneSelectNode(nodeId: string) {
    editorStore.setSelectedNode(nodeId)

    if (window.matchMedia('(width < 640px)').matches) {
      setIsMobileInspectorOpen(true)
    }
  }

  const inspectorProps = {
    onShapeFillChange: editorStore.setShapeFillColor,
    onShapeFillOpacityChange: editorStore.setShapeFillOpacity,
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
      <div className={styles.toolbar}>
        <EditorToolbar
          isSavingVersion={isSavingProjectVersion}
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
      </div>
      <aside className={styles.layers}>
        <LayersPanel
          expandedGroupIds={editorStore.expandedGroupIds}
          nodes={editorStore.document.layers}
          onSelectNode={editorStore.setSelectedNode}
          onToggleGroup={editorStore.toggleGroup}
          onToggleVisibility={handleToggleVisibility}
          selectedNodeId={editorStore.selectedNodeId}
        />
      </aside>
      <div className={styles.scene}>
        <EditorScene
          document={editorStore.document}
          onSelectNode={handleSceneSelectNode}
          renderVersion={sceneRenderVersion}
        />
      </div>
      <aside className={styles.inspector}>
        <InspectorPanel {...inspectorProps} />
      </aside>
      <div className={styles.status}>
        <EditorStatusBar />
      </div>
      <MobileInspector
        {...inspectorProps}
        isOpen={isMobileInspectorOpen}
        onClose={() => setIsMobileInspectorOpen(false)}
      />
    </section>
  )
})
