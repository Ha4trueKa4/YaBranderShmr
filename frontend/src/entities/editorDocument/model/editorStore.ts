/**
 * Состояние взаимодействия с редактором.
 * Здесь появятся document, undo/redo, viewport и actions; UI не должен хранить их локально.
 */
export interface EditorUiState {
  selectedLayerId: string | null
  zoom: number
}
