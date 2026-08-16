import { api, parseApiResponse } from '@shared/api/api'
import {
  type AuthResponse,
  type RegisterRequest,
  zPostAuthRegisterResponse,
  zRegisterRequest,
} from '@shared/api/types'

export async function register(body: RegisterRequest): Promise<AuthResponse> {
  const validBody = zRegisterRequest.parse(body)
  const response = await api<unknown, RegisterRequest>('/auth/register', {
    method: 'POST',
    body: validBody,
  })

  return parseApiResponse(zPostAuthRegisterResponse, response, '/auth/register')
}
