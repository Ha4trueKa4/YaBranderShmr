import { api, parseApiResponse } from '@shared/api/api'
import { z } from 'zod'

const zModelsResponse = z.object({
  data: z.array(
    z.object({
      id: z.string(),
      owned_by: z.string(),
    }),
  ),
})

export interface GenerationModel {
  id: string
  owner: string
}

function getGenerationModelId(modelUri: string): string | null {
  const match = /^gpt:\/\/[^/]+\/(.+)$/.exec(modelUri)

  return match?.[1] ?? null
}

export async function getModels(): Promise<GenerationModel[]> {
  // FIXME: Mark as requiresAuth after the API accepts valid access tokens here.
  const response = await api<unknown>('/models')
  const models = parseApiResponse(zModelsResponse, response, '/models')

  return models.data.flatMap(({ id, owned_by }) => {
    const modelId = getGenerationModelId(id)

    return modelId ? [{ id: modelId, owner: owned_by }] : []
  })
}
