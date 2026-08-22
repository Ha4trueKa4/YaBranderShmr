import { setNodeVisibility } from '../commands/setNodeVisibility'
import { toggleNodeVisibility } from '../commands/toggleNodeVisibility'
import type { SvgEditingContext } from './editingContext'

export function createLayerVisibilityEditing({
  applyDocumentChange,
  getDocument,
}: SvgEditingContext) {
  return {
    setNodeVisibility(nodeId: string, isVisible: boolean) {
      applyDocumentChange(() =>
        setNodeVisibility(getDocument().layers, nodeId, isVisible),
      )
    },

    toggleNodeVisibility(nodeId: string) {
      applyDocumentChange(() =>
        toggleNodeVisibility(getDocument().layers, nodeId),
      )
    },
  }
}
