import { BottomSheet, Button } from '@shared/ui'
import type { ComponentProps } from 'react'

import { InspectorPanel } from '../inspectorPanel/InspectorPanel'

interface MobileInspectorProps extends ComponentProps<typeof InspectorPanel> {
  isOpen: boolean
  onClose: () => void
}

export function MobileInspector({
  isOpen,
  onClose,
  ...inspectorProps
}: MobileInspectorProps) {
  return (
    <BottomSheet
      actions={
        <Button onClick={onClose} size="sm" variant="ghost">
          Done
        </Button>
      }
      isOpen={isOpen}
      onClose={onClose}
      title="Properties"
    >
      <InspectorPanel {...inspectorProps} />
    </BottomSheet>
  )
}
