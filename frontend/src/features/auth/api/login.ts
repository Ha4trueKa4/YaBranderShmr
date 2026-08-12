import { api } from '@shared/api/api'
import {
  type AuthResponse,
  type LoginRequest,
  zLoginRequest,
  zPostAuthLoginResponse,
} from '@shared/api/types'

export async function login(body: LoginRequest): Promise<AuthResponse> {
  const validBody = zLoginRequest.parse(body)
  const response = await api<unknown, LoginRequest>('/auth/login', {
    method: 'POST',
    body: validBody,
  })

  return zPostAuthLoginResponse.parse(response)
}
