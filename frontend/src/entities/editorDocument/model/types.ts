export type Paint =
  | { type: 'color'; value: string }
  | { type: 'gradient'; gradientId: string }
  | { type: 'none' }

export type Transform = {
  rotation: number
  rotationCenter?: {
    x: number
    y: number
  }
  scaleX: number
  scaleY: number
  x: number
  y: number
}

export type ShapeStyle = {
  fill: Paint
  fillOpacity: number
  stroke: Paint
  strokeDasharray?: string
  strokeLineCap?: 'butt' | 'round' | 'square'
  strokeLineJoin?: 'bevel' | 'miter' | 'round'
  strokeOpacity: number
  strokeWidth: number
}

export type TextStyle = {
  fill: Paint
  fontFamily: string
  fontSize: number
  fontStyle?: 'italic' | 'normal'
  fontWeight?: number
  letterSpacing?: number
  lineHeight?: number
  textAnchor?: 'end' | 'middle' | 'start'
}

export type RectGeometry = {
  height: number
  rx?: number
  ry?: number
  type: 'rect'
  width: number
  x: number
  y: number
}

export type CircleGeometry = {
  cx: number
  cy: number
  r: number
  type: 'circle'
}

export type PathGeometry = {
  d: string
  type: 'path'
}

export type EllipseGeometry = {
  cx: number
  cy: number
  rx: number
  ry: number
  type: 'ellipse'
}

export type LineGeometry = {
  type: 'line'
  x1: number
  x2: number
  y1: number
  y2: number
}

export type ShapeGeometry =
  | RectGeometry
  | CircleGeometry
  | PathGeometry
  | EllipseGeometry
  | LineGeometry
export type EditorShapeType = ShapeGeometry['type']
export type EditorElementType = 'shape' | 'text'
export type EditorNodeType = EditorElementType | 'group' | 'raw'

export type EditorElementBase = {
  id: string
  isLocked: boolean
  isVisible: boolean
  name: string
  opacity: number
  role?: 'background' | 'content'
  transform: Transform
}

export type EditorShape = EditorElementBase & {
  style: ShapeStyle
  type: 'shape'
} & (
    | { geometry: RectGeometry; shapeType: 'rect' }
    | { geometry: CircleGeometry; shapeType: 'circle' }
    | { geometry: PathGeometry; shapeType: 'path' }
    | { geometry: EllipseGeometry; shapeType: 'ellipse' }
    | { geometry: LineGeometry; shapeType: 'line' }
  )

export type EditorText = EditorElementBase & {
  style: TextStyle
  text: string
  type: 'text'
  x: number
  y: number
}

export type EditorElement = EditorShape | EditorText

export type EditorGroup = EditorElementBase & {
  children: EditorNode[]
  type: 'group'
}

export type EditorRawNode = EditorElementBase & {
  rawSvg: string
  type: 'raw'
}

export type EditorNode = EditorGroup | EditorElement | EditorRawNode

export type EditorElementByType<TType extends EditorElementType> = Extract<
  EditorElement,
  { type: TType }
>

/**
 * Градиенты не являются слоями: MVP только сохраняет их, чтобы SVG не терял
 * внешний вид. Редактирование точек и stops добавим при необходимости.
 */
export type GradientStop = {
  color: string
  offset: string
  opacity?: number
}

export type GradientDefinition = {
  attributes: Record<string, string>
  id: string
  stops: GradientStop[]
  type: 'linearGradient' | 'radialGradient'
}

export type EditorDefinitions = {
  gradients: GradientDefinition[]
  rawSvg?: string
}

export interface EditorDocument {
  definitions?: EditorDefinitions
  width: number
  height: number
  viewBoxX?: number
  viewBoxY?: number
  layers: EditorNode[]
}
