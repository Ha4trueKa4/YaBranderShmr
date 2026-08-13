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
  // TODO: получать документ из загруженного Project (`project.data`) и создавать store с ним.
  // Сейчас editorFixture временно заменяет ответ API, чтобы можно было проверять UI.
  const [editorStore] = useState(
    () => new EditorStore(editorFixture, 'icon-shape', ['logo']),
  )

  function handleToolbarAction() {
    // TODO: подключить действия после реализации editorStore.
  }

  function handleProjectNameSubmit(_projectName: string) {
    // TODO: сохранить название проекта после подключения editorStore.
  }

  return (
    <section className={styles.workspace}>
      <div className={styles.toolbar}>
        <EditorToolbar
          onEditProjectName={handleProjectNameSubmit}
          onExport={handleToolbarAction}
          onOpenProfile={() => navigate(AppRoute.Profile)}
          onPanTool={handleToolbarAction}
          onRedo={handleToolbarAction}
          onSaveVersion={handleToolbarAction}
          onSelectTool={handleToolbarAction}
          onUndo={handleToolbarAction}
          onZoomIn={handleToolbarAction}
          onZoomOut={handleToolbarAction}
          // TODO: передавать название загруженного Project вместо временного значения.
          projectName="Orion Labs"
        />
      </div>
      <aside className={styles.layers}>
        <LayersPanel
          expandedGroupIds={editorStore.expandedGroupIds}
          nodes={editorStore.document.layers}
          onSelectLayer={editorStore.setSelectedLayer}
          onToggleGroup={editorStore.toggleGroup}
          selectedLayerId={editorStore.selectedLayerId}
        />
      </aside>
      <div className={styles.scene}>
        <EditorScene />
      </div>
      <aside className={styles.inspector}>
        <InspectorPanel selectedLayer={editorStore.selectedLayer} />
      </aside>
      <div className={styles.status}>
        <EditorStatusBar />
      </div>
    </section>
  )
})
