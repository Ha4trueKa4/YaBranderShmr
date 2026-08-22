import type { EditorGroup } from '@entities/editorDocument'
import { useTranslation } from 'react-i18next'

import { PropertyField } from '../components/PropertyField'

interface GroupPropertiesProps {
  layer: EditorGroup
}

export function GroupProperties({ layer }: GroupPropertiesProps) {
  const { t } = useTranslation()

  return (
    <>
      <PropertyField label={t('Name')} value={layer.name} />
      <PropertyField label={t('X')} value={layer.transform.x} />
      <PropertyField label={t('Y')} value={layer.transform.y} />
      <PropertyField label={t('Opacity')} value={layer.opacity} />
    </>
  )
}
