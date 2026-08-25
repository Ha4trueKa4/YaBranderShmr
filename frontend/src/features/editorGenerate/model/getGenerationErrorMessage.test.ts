import { describe, expect, it } from 'vitest'

import {
  getGenerationErrorMessage,
  isContentPolicyRejection,
} from './getGenerationErrorMessage'

describe('getGenerationErrorMessage', () => {
  it('maps a content moderation rejection to a safe client message', () => {
    const error = { message: 'Content moderation policy violation' }

    expect(isContentPolicyRejection(error)).toBe(true)
    expect(getGenerationErrorMessage(error)).toBe(
      'The request was rejected due to content restrictions. Please change the description and try again.',
    )
  })

  it('does not treat unrelated errors as content policy rejections', () => {
    expect(isContentPolicyRejection({ message: 'Provider unavailable' })).toBe(
      false,
    )
  })
})
