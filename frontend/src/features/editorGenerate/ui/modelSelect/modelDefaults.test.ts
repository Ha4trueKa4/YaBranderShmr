import { describe, expect, it } from 'vitest'

import {
  DEFAULT_GENERATION_MODEL,
  getDefaultGenerationModel,
} from './modelDefaults'

describe('getDefaultGenerationModel', () => {
  it('prefers GPT Luna when it is available', () => {
    expect(
      getDefaultGenerationModel([
        { id: 'deepseek-v4-flash/latest', owner: 'DeepSeek' },
        { id: 'gpt-5.6-luna', owner: 'OpenAI' },
      ]),
    ).toBe('gpt-5.6-luna')
  })

  it('falls back to an available DeepSeek model', () => {
    expect(
      getDefaultGenerationModel([
        { id: 'deepseek-v4-flash/latest', owner: 'DeepSeek' },
      ]),
    ).toBe('deepseek-v4-flash/latest')
  })

  it('uses the safe DeepSeek fallback until the list is available', () => {
    expect(getDefaultGenerationModel([])).toBe(DEFAULT_GENERATION_MODEL)
  })
})
