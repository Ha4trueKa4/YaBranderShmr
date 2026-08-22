import { createDefinitionsEditing } from './definitionsEditing'
import type { SvgEditingContext } from './editingContext'
import { createGroupEditing } from './groupEditing'
import { createLayerVisibilityEditing } from './layerVisibility'
import { createShapeEditing } from './shapeEditing'
import { createTextEditing } from './textEditing'

export function createSvgNodeEditing(context: SvgEditingContext) {
  return {
    ...createDefinitionsEditing(context),
    ...createGroupEditing(context),
    ...createLayerVisibilityEditing(context),
    ...createShapeEditing(context),
    ...createTextEditing(context),
  }
}

export type SvgNodeEditing = ReturnType<typeof createSvgNodeEditing>
