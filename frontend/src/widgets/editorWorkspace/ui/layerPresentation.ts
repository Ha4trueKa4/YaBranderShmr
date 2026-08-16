import type { EditorNode, EditorShapeType } from '@entities/editorDocument'
import type { LucideIcon } from 'lucide-react'
import {
  Circle,
  Folder,
  Lock,
  Minus,
  PenTool,
  Square,
  Type,
} from 'lucide-react'

export type LayerPresentation = {
  icon: LucideIcon
  label: string
}

const presentationByNodeType: Record<
  Exclude<EditorNode['type'], 'shape'>,
  LayerPresentation
> = {
  group: { icon: Folder, label: 'Group' },
  raw: { icon: Lock, label: 'Locked SVG' },
  text: { icon: Type, label: 'Text' },
}

const presentationByShapeType = {
  circle: { icon: Circle, label: 'Circle' },
  ellipse: { icon: Circle, label: 'Ellipse' },
  line: { icon: Minus, label: 'Line' },
  path: { icon: PenTool, label: 'Path' },
  rect: { icon: Square, label: 'Rectangle' },
} satisfies Record<EditorShapeType, LayerPresentation>

export function getLayerPresentation(node: EditorNode): LayerPresentation {
  if (node.type !== 'shape') {
    return presentationByNodeType[node.type]
  }

  return presentationByShapeType[node.shapeType]
}
