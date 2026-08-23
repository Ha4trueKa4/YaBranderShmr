import { IconButton } from '@shared/ui'
import type { CanvasTool } from '@widgets/editorScene'
import { ArrowLeft } from 'lucide-react'
import { CanvasControls } from './components/CanvasControls'
import { ProjectActions } from './components/ProjectActions'
import { ProjectTitle } from './components/ProjectTitle'
import styles from './EditorToolbar.module.css'

export interface EditorToolbarProps {
  canEdit: boolean
  canRedo: boolean
  canUndo: boolean
  activeTool: CanvasTool
  isCreatingProjectCopy: boolean
  isProjectPublic: boolean
  isSavingVersion: boolean
  onBack: () => void
  onCreateCopy: () => void
  onEditProjectName: (projectName: string) => Promise<void>
  onExport: () => void
  onOpenProfile: () => void
  onOpenVersionHistory: () => void
  onToggleProjectVisibility: () => void
  onToolChange: (tool: CanvasTool) => void
  onRedo: () => void
  onSaveVersion: () => void
  onUndo: () => void
  onZoomIn: () => void
  onZoomOut: () => void
  projectName: string
  zoom: number
}

export function EditorToolbar({
  canEdit,
  canRedo,
  canUndo,
  activeTool,
  isCreatingProjectCopy,
  isProjectPublic,
  isSavingVersion,
  onBack,
  onCreateCopy,
  onEditProjectName,
  onExport,
  onOpenProfile,
  onOpenVersionHistory,
  onToggleProjectVisibility,
  onToolChange,
  onRedo,
  onSaveVersion,
  onUndo,
  onZoomIn,
  onZoomOut,
  projectName,
  zoom,
}: EditorToolbarProps) {
  return (
    <header className={styles.toolbar}>
      <IconButton
        aria-label="Back"
        className={styles.backButton}
        onClick={onBack}
      >
        <ArrowLeft className={styles.backIcon} />
      </IconButton>
      <ProjectTitle
        canEdit={canEdit}
        isProjectPublic={isProjectPublic}
        isVisibilityUpdating={isSavingVersion}
        onSubmit={onEditProjectName}
        onToggleVisibility={onToggleProjectVisibility}
        title={projectName}
      />
      {canEdit && (
        <CanvasControls
          activeTool={activeTool}
          canRedo={canRedo}
          canUndo={canUndo}
          onToolChange={onToolChange}
          onRedo={onRedo}
          onUndo={onUndo}
          onZoomIn={onZoomIn}
          onZoomOut={onZoomOut}
          zoom={zoom}
        />
      )}
      <ProjectActions
        canEdit={canEdit}
        isCreatingProjectCopy={isCreatingProjectCopy}
        isSavingVersion={isSavingVersion}
        onCreateCopy={onCreateCopy}
        onExport={onExport}
        onOpenProfile={onOpenProfile}
        onOpenVersionHistory={onOpenVersionHistory}
        onSaveVersion={onSaveVersion}
      />
    </header>
  )
}
