import { useState } from 'react'
import { CanvasControls, type CanvasTool } from './components/CanvasControls'
import { ProjectActions } from './components/ProjectActions'
import { ProjectTitle } from './components/ProjectTitle'
import styles from './EditorToolbar.module.css'

export interface EditorToolbarProps {
  canEdit: boolean
  isCreatingProjectCopy: boolean
  isSavingVersion: boolean
  onCreateCopy: () => void
  onEditProjectName: (projectName: string) => Promise<void>
  onExport: () => void
  onOpenProfile: () => void
  onPanTool: () => void
  onRedo: () => void
  onSaveVersion: () => void
  onSelectTool: () => void
  onUndo: () => void
  onZoomIn: () => void
  onZoomOut: () => void
  projectName: string
}

export function EditorToolbar({
  canEdit,
  isCreatingProjectCopy,
  isSavingVersion,
  onCreateCopy,
  onEditProjectName,
  onExport,
  onOpenProfile,
  onPanTool,
  onRedo,
  onSaveVersion,
  onSelectTool,
  onUndo,
  onZoomIn,
  onZoomOut,
  projectName,
}: EditorToolbarProps) {
  const [activeTool, setActiveTool] = useState<CanvasTool>('select')

  function handleSelectTool() {
    setActiveTool('select')
    onSelectTool()
  }

  function handlePanTool() {
    setActiveTool('pan')
    onPanTool()
  }

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
          onPanTool={handlePanTool}
          onRedo={onRedo}
          onSelectTool={handleSelectTool}
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
