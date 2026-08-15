import { parseSvg } from '@entities/editorDocument'

import { mockTaxiSvg } from './mockTaxiSvg'

/**
 * Временная замена `project.data` для UI редактора, пока проект не загружается с API.
 * Мок намеренно проходит тот же путь, что и будущий ответ API: SVG → EditorDocument.
 */
export const editorFixture = parseSvg(mockTaxiSvg)
