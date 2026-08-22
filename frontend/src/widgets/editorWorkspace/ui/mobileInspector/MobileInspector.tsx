import { BottomSheet, Button } from '@shared/ui'
import type { ComponentProps } from 'react'
import { useTranslation } from 'react-i18next'

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
  const { t } = useTranslation()

  return (
    <BottomSheet
      actions={
        <Button onClick={onClose} size="sm" variant="ghost">
          {t('Done')}
        </Button>
      }
      isOpen={isOpen}
      onClose={onClose}
      title={t('Properties')}
    >
      <InspectorPanel {...inspectorProps} />
    </BottomSheet>
  )
}
