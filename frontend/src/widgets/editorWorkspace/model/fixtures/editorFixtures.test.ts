import { describe, expect, it } from 'vitest'

import {
  createEditorFixtureDocument,
  editorFixtures,
  getEditorFixture,
} from '.'

describe('editor fixtures', () => {
  it('contains contract-ready SVG documents that are parsed by the editor', () => {
    for (const fixture of editorFixtures) {
      expect(createEditorFixtureDocument(fixture.id)).toMatchObject({
        height: expect.any(Number),
        layers: expect.any(Array),
        width: expect.any(Number),
      })
    }
  })

  it('keeps the simple taxi fixture available as the initial editor mock', () => {
    expect(getEditorFixture('taxi-simple')).toMatchObject({
      initiallySelectedNodeId: 'taxi-body',
      name: 'Taxi — простой',
      svg: expect.stringContaining('id="taxi-body"'),
    })
  })
})
