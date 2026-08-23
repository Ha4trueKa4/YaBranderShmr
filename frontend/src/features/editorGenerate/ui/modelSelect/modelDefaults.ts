import type { GenerationModel } from '../../api/getModels'

export const DEFAULT_GENERATION_MODEL = 'deepseek-v4-flash/latest'

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
    models.find(isDeepSeek)?.id ??
    DEFAULT_GENERATION_MODEL
  )
}
