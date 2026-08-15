import type { EditorGroup } from '@entities/editorDocument'

import { PropertyField } from '../components/PropertyField'

interface GroupPropertiesProps {
  layer: EditorGroup
}

export function GroupProperties({ layer }: GroupPropertiesProps) {
  return (
    <>
      <PropertyField label="Name" value={layer.name} />
      <PropertyField label="X" value={layer.transform.x} />
      <PropertyField label="Y" value={layer.transform.y} />
      <PropertyField label="Opacity" value={layer.opacity} />
    </>
  )
}
