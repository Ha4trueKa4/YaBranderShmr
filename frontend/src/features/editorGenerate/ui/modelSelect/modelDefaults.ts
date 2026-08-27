import type { GenerationModel } from '../../api/getModels'

export const DEFAULT_GENERATION_MODEL = 'deepseek-v4-flash/latest'

function isRecraft(model: GenerationModel) {
  const modelId = model.id.toLowerCase()

  return modelId.includes('recraft')
}

function isGptLuna(model: GenerationModel) {
  const modelId = model.id.toLowerCase()

  return modelId.includes('gpt') && modelId.includes('luna')
}

function isDeepSeek(model: GenerationModel) {
  return model.id.toLowerCase().includes('deepseek')
}

export function getDefaultGenerationModel(models: GenerationModel[]): string {
  return (
    models.find(isGptLuna)?.id ??
    models.find(isRecraft)?.id ??
    models.find(isDeepSeek)?.id ??
    DEFAULT_GENERATION_MODEL
  )
}

export function getDefaultChatGenerationModel(
  models: GenerationModel[],
): string {
  return (
    models.find(isRecraft)?.id ??
    models.find(isGptLuna)?.id ??
    models.find(isDeepSeek)?.id ??
    DEFAULT_GENERATION_MODEL
  )
}
