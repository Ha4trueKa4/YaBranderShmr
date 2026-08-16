import { useState } from 'react'
import { CanvasControls, type CanvasTool } from './components/CanvasControls'
import { ProjectActions } from './components/ProjectActions'
import { ProjectTitle } from './components/ProjectTitle'
import styles from './EditorToolbar.module.css'

export interface EditorToolbarProps {
  isSavingVersion: boolean
  onEditProjectName: (projectName: string) => void
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
  isSavingVersion,
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
      <ProjectTitle onSubmit={onEditProjectName} title={projectName} />
      <CanvasControls
        activeTool={activeTool}
        onPanTool={handlePanTool}
        onRedo={onRedo}
        onSelectTool={handleSelectTool}
        onUndo={onUndo}
        onZoomIn={onZoomIn}
        onZoomOut={onZoomOut}
      />
      <ProjectActions
        isSavingVersion={isSavingVersion}
        onExport={onExport}
        onOpenProfile={onOpenProfile}
        onSaveVersion={onSaveVersion}
      />
    </header>
  )
}
