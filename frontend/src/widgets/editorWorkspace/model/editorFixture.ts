import {
  createEditorFixtureDocument,
  type EditorFixtureId,
  getEditorFixture,
} from './fixtures'

/**
 * Временная замена `project.data` для UI редактора, пока проект не загружается с API.
 * Чтобы временно сменить макет на холсте, достаточно поменять ID ниже:
 * `taxi-simple`, `taxi-detailed`, `taxi-qwen` или `profile-badge`.
 */
export const activeEditorFixtureId: EditorFixtureId = 'taxi-detailed'

const activeEditorFixture = getEditorFixture(activeEditorFixtureId)

export const editorFixture = createEditorFixtureDocument(activeEditorFixtureId)
export const editorFixtureInitialState = {
  expandedGroupIds: activeEditorFixture.initiallyExpandedGroupIds,
  selectedNodeId: activeEditorFixture.initiallySelectedNodeId,
}
