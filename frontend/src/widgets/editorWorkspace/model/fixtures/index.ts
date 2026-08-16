import type { EditorDocument } from '@entities/editorDocument'
import { parseSvg } from '@entities/editorDocument'

import { profileBadgeSvg } from './profileBadgeSvg'
import { taxiDetailedSvg } from './taxiDetailedSvg'
import { taxiQwenSvg } from './taxiQwenSvg'
import { taxiSimpleSvg } from './taxiSimpleSvg'

export type EditorFixtureId =
  | 'profile-badge'
  | 'taxi-detailed'
  | 'taxi-qwen'
  | 'taxi-simple'

export type EditorFixture = {
  initiallyExpandedGroupIds: readonly string[]
  initiallySelectedNodeId: string | null
  id: EditorFixtureId
  name: string
  svg: string
}

/** Полный список контрактных SVG для ручной проверки холста и панели слоёв. */
export const editorFixtures: readonly EditorFixture[] = [
  {
    id: 'taxi-simple',
    initiallyExpandedGroupIds: ['taxi', 'taxi-sign'],
    initiallySelectedNodeId: 'taxi-body',
    name: 'Taxi — простой',
    svg: taxiSimpleSvg,
  },
  {
    id: 'taxi-detailed',
    initiallyExpandedGroupIds: ['car-body', 'wheels'],
    initiallySelectedNodeId: 'body-main',
    name: 'Taxi — подробный',
    svg: taxiDetailedSvg,
  },
  {
    id: 'taxi-qwen',
    initiallyExpandedGroupIds: ['qwen-taxi', 'qwen-taxi-sign'],
    initiallySelectedNodeId: 'qwen-body-base',
    name: 'Taxi — Qwen',
    svg: taxiQwenSvg,
  },
  {
    id: 'profile-badge',
    initiallyExpandedGroupIds: ['badge-avatar', 'badge-copy'],
    initiallySelectedNodeId: 'badge-avatar-circle',
    name: 'Профильная карточка',
    svg: profileBadgeSvg,
  },
]

export function getEditorFixture(id: EditorFixtureId): EditorFixture {
  const fixture = editorFixtures.find((item) => item.id === id)

  if (!fixture) {
    throw new Error(`Unknown editor fixture: ${id}`)
  }

  return fixture
}

/** Используется UI до подключения `project.data` из API. */
export function createEditorFixtureDocument(
  id: EditorFixtureId,
): EditorDocument {
  return parseSvg(getEditorFixture(id).svg)
}
