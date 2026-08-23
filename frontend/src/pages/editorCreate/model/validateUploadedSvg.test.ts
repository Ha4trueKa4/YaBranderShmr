import { describe, expect, it } from 'vitest'

import {
  getUploadedSvgValidationError,
  MAX_UPLOADED_SVG_LENGTH,
} from './validateUploadedSvg'

describe('getUploadedSvgValidationError', () => {
  it('accepts SVG markup in SVG and TXT files', () => {
    const svg = '<svg xmlns="http://www.w3.org/2000/svg"></svg>'

    expect(getUploadedSvgValidationError('logo.svg', svg)).toBeUndefined()
    expect(getUploadedSvgValidationError('logo.txt', svg)).toBeUndefined()
  })

  it('rejects unsupported files and non-SVG text', () => {
    expect(getUploadedSvgValidationError('logo.png', '<svg></svg>')).toBe(
      'invalidFileType',
    )
    expect(getUploadedSvgValidationError('note.txt', 'just some text')).toBe(
      'invalidSvg',
    )
  })

  it('uses the backend limit of 5000 UTF-8 characters', () => {
    expect(
      getUploadedSvgValidationError(
        'logo.svg',
        `<svg>${'😀'.repeat(MAX_UPLOADED_SVG_LENGTH)}</svg>`,
      ),
    ).toBe('tooLong')
  })
})
