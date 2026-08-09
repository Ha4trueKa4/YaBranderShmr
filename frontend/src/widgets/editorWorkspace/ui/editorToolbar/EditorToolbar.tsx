import { CanvasControls } from './components/CanvasControls'
import { ProjectActions } from './components/ProjectActions'
import { ProjectTitle } from './components/ProjectTitle'
import styles from './EditorToolbar.module.css'
import {useToast} from "@shared";

export interface EditorToolbarProps {
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
    const toast = useToast();
    toast.error("error")
    toast.info("info")
    toast.error("error")
  return (
    <header className={styles.toolbar}>
      <ProjectTitle onSubmit={onEditProjectName} title={projectName} />
      <CanvasControls
        onPanTool={onPanTool}
        onRedo={onRedo}
        onSelectTool={onSelectTool}
        onUndo={onUndo}
        onZoomIn={onZoomIn}
        onZoomOut={onZoomOut}
      />
      <ProjectActions
        onExport={onExport}
        onOpenProfile={onOpenProfile}
        onSaveVersion={onSaveVersion}
      />
    </header>
  )
}
