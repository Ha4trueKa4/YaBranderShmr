import { AppRoute } from '@app/providers/router/routes'
import { EditorStore } from '@entities/editorDocument'
import { EditorScene } from '@widgets/editorScene'
import { observer } from 'mobx-react-lite'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { editorFixture } from '../model/editorFixture'
import styles from './EditorWorkspace.module.css'
import { EditorStatusBar } from './editorStatusBar/EditorStatusBar'
import { EditorToolbar } from './editorToolbar/EditorToolbar'
import { InspectorPanel } from './inspectorPanel/InspectorPanel'
import { LayersPanel } from './layersPanel/LayersPanel'

export const EditorWorkspace = observer(function EditorWorkspace() {
  const navigate = useNavigate()
  const [projectName, setProjectName] = useState('Orion Labs')
  // TODO: получать документ из загруженного Project (`project.data`) и создавать store с ним.
  // Сейчас editorFixture временно заменяет ответ API и собирается через parseSvg.
  const [editorStore] = useState(
    () => new EditorStore(editorFixture, 'taxi-body', ['taxi', 'taxi-sign']),
  )

  function handleToolbarAction() {
    // TODO: подключить действия после реализации editorStore.
  }

  function handleProjectNameSubmit(nextProjectName: string) {
    // TODO: сохранять название через API, когда PATCH /projects/{id}
    // будет поддерживать поле name.
    setProjectName(nextProjectName)
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
          // TODO: передавать название загруженного Project вместо временного значения.
          projectName={projectName}
        />
      </div>
      <aside className={styles.layers}>
        <LayersPanel
          expandedGroupIds={editorStore.expandedGroupIds}
          nodes={editorStore.document.layers}
          onSelectNode={editorStore.setSelectedNode}
          onToggleGroup={editorStore.toggleGroup}
          onToggleVisibility={editorStore.toggleNodeVisibility}
          selectedNodeId={editorStore.selectedNodeId}
        />
      </aside>
      <div className={styles.scene}>
        <EditorScene document={editorStore.document} />
      </div>
      <aside className={styles.inspector}>
        <InspectorPanel selectedNode={editorStore.selectedNode} />
      </aside>
      <div className={styles.status}>
        <EditorStatusBar />
      </div>
    </section>
  )
})
