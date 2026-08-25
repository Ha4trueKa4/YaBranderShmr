import { describe, expect, it } from 'vitest'

import {
  DEFAULT_GENERATION_MODEL,
  getDefaultChatGenerationModel,
  getDefaultGenerationModel,
} from './modelDefaults'

describe('getDefaultGenerationModel', () => {
  it('prefers Recraft when it is available', () => {
    expect(
      getDefaultGenerationModel([
        { id: 'deepseek-v4-flash/latest', owner: 'DeepSeek' },
        { id: 'gpt-5.6-luna', owner: 'OpenAI' },
        { id: 'recraftv2_vector', owner: 'Recraft' },
      ]),
    ).toBe('recraftv2_vector')
  })

  it('falls back to GPT Luna when Recraft is unavailable', () => {
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

describe('getDefaultChatGenerationModel', () => {
  it('prefers GPT Luna for chat generation', () => {
    expect(
      getDefaultChatGenerationModel([
        { id: 'recraftv2_vector', owner: 'Recraft' },
        { id: 'gpt-5.6-luna', owner: 'OpenAI' },
      ]),
    ).toBe('gpt-5.6-luna')
  })

  it('falls back to Recraft when GPT Luna is unavailable', () => {
    expect(
      getDefaultChatGenerationModel([
        { id: 'recraftv2_vector', owner: 'Recraft' },
      ]),
    ).toBe('recraftv2_vector')
  })
})
