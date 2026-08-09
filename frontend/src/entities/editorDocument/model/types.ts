export type EditorLayerType = 'background' | 'shape' | 'text'

interface EditorLayerBase {
  id: string
  name: string
}

export type BackgroundLayer = EditorLayerBase & {
  fill: string
  type: 'background'
}

export type ShapeLayer = EditorLayerBase & {
  fill: string
  stroke?: string
  type: 'shape'
  x: number
  y: number
}

export type TextLayer = EditorLayerBase & {
  fontFamily: string
  fontSize: number
  text: string
  type: 'text'
}

/**
 * Дискриминируемый набор слоёв документа.
 * При добавлении нового слоя дополнить этот union и inspectorByType.
 */
export type EditorLayer = BackgroundLayer | ShapeLayer | TextLayer

export type EditorLayerByType<TType extends EditorLayerType> = Extract<
  EditorLayer,
  { type: TType }
>

export interface EditorDocument {
  height: number
  layers: EditorLayer[]
  width: number
}
