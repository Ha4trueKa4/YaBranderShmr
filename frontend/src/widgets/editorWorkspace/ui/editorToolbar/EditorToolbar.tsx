import type { CanvasTool } from '@widgets/editorScene'
import { CanvasControls } from './components/CanvasControls'
import { ProjectActions } from './components/ProjectActions'
import { ProjectTitle } from './components/ProjectTitle'
import styles from './EditorToolbar.module.css'

export interface EditorToolbarProps {
  canEdit: boolean
  activeTool: CanvasTool
  isCreatingProjectCopy: boolean
  isSavingVersion: boolean
  onCreateCopy: () => void
  onEditProjectName: (projectName: string) => Promise<void>
  onExport: () => void
  onOpenProfile: () => void
  onToolChange: (tool: CanvasTool) => void
  onRedo: () => void
  onSaveVersion: () => void
  onUndo: () => void
  onZoomIn: () => void
  onZoomOut: () => void
  projectName: string
}

export function EditorToolbar({
  canEdit,
  activeTool,
  isCreatingProjectCopy,
  isSavingVersion,
  onCreateCopy,
  onEditProjectName,
  onExport,
  onOpenProfile,
  onToolChange,
  onRedo,
  onSaveVersion,
  onUndo,
  onZoomIn,
  onZoomOut,
  projectName,
}: EditorToolbarProps) {
  return (
    <header className={styles.toolbar}>
      <ProjectTitle
        canEdit={canEdit}
        onSubmit={onEditProjectName}
        title={projectName}
      />
      {canEdit && (
        <CanvasControls
          activeTool={activeTool}
          onToolChange={onToolChange}
          onRedo={onRedo}
          onUndo={onUndo}
          onZoomIn={onZoomIn}
          onZoomOut={onZoomOut}
        />
      )}
      <ProjectActions
        canEdit={canEdit}
        isCreatingProjectCopy={isCreatingProjectCopy}
        isSavingVersion={isSavingVersion}
        onCreateCopy={onCreateCopy}
        onExport={onExport}
        onOpenProfile={onOpenProfile}
        onSaveVersion={onSaveVersion}
      />
    </header>
  )
}
