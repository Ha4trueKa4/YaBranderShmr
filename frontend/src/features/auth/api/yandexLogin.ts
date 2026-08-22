import { api, parseApiResponse } from '@shared/api/api'
import {
  type AuthResponse,
  type YandexAuthRequest,
  zPostAuthYandexResponse,
  zYandexAuthRequest,
} from '@shared/api/types'

export async function loginWithYandex(
  body: YandexAuthRequest,
): Promise<AuthResponse> {
  const validBody = zYandexAuthRequest.parse(body)
  const response = await api<unknown, YandexAuthRequest>('/auth/yandex', {
    method: 'POST',
    body: validBody,
  })

  return parseApiResponse(zPostAuthYandexResponse, response, '/auth/yandex')
}
