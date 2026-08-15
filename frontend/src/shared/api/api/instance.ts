import { createApiClient } from './client.ts'
import { getStoredSession } from './session.ts'

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1'

export const api = createApiClient(API_URL, {
  getAccessToken: () => getStoredSession()?.accessToken ?? null,
})
