import { AppRoute } from '@app/providers/router/routes'
import { type EditorDocument, EditorStore } from '@entities/editorDocument'
import { EditorScene } from '@widgets/editorScene'
import { observer } from 'mobx-react-lite'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styles from './EditorWorkspace.module.css'
import { EditorStatusBar } from './editorStatusBar/EditorStatusBar'
import { EditorToolbar } from './editorToolbar/EditorToolbar'
import { InspectorPanel } from './inspectorPanel/InspectorPanel'
import { LayersPanel } from './layersPanel/LayersPanel'

interface EditorWorkspaceProps {
  document: EditorDocument
  projectName: string
}

export const EditorWorkspace = observer(function EditorWorkspace({
  document,
  projectName: initialProjectName,
}: EditorWorkspaceProps) {
  const navigate = useNavigate()
  const [projectName, setProjectName] = useState(initialProjectName)
  const [editorStore] = useState(() => new EditorStore(document))
  const [sceneRenderVersion, setSceneRenderVersion] = useState(0)

  function handleToolbarAction() {
    // TODO: подключить действия после реализации editorStore.
  }

  function handleProjectNameSubmit(nextProjectName: string) {
    // TODO: сохранять название через API, когда PATCH /projects/{id}
    // будет поддерживать поле name.
    setProjectName(nextProjectName)
  }

  function handleToggleVisibility(nodeId: string) {
    editorStore.toggleNodeVisibility(nodeId)
    setSceneRenderVersion((version) => version + 1)
  }

  return (
    <section className={styles.workspace}>
      <div className={styles.toolbar}>
        <EditorToolbar
          onEditProjectName={handleProjectNameSubmit}
          onExport={handleToolbarAction}
          onOpenProfile={() =>
            navigate(AppRoute.Profile, { viewTransition: true })
          }
          onPanTool={handleToolbarAction}
          onRedo={handleToolbarAction}
          onSaveVersion={handleToolbarAction}
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
          onSelectNode={editorStore.setSelectedNode}
          renderVersion={sceneRenderVersion}
        />
      </div>
      <aside className={styles.inspector}>
        <InspectorPanel
          onShapeFillChange={editorStore.setShapeFillColor}
          onShapeFillOpacityChange={editorStore.setShapeFillOpacity}
          onTextAnchorChange={editorStore.setTextAnchor}
          onTextContentChange={editorStore.setTextContent}
          onTextFillColorChange={editorStore.setTextFillColor}
          onTextFontFamilyChange={editorStore.setTextFontFamily}
          onTextFontSizeChange={editorStore.setTextFontSize}
          onTextFontStyleChange={editorStore.setTextFontStyle}
          onTextFontWeightChange={editorStore.setTextFontWeight}
          onTextPositionChange={editorStore.setTextPosition}
          selectedNode={editorStore.selectedNode}
        />
      </aside>
      <div className={styles.status}>
        <EditorStatusBar />
      </div>
    </section>
  )
})
