import { createApiClient } from './client.ts'
import { getStoredSession } from './session.ts'

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1'

let unauthorizedHandler: (() => void) | undefined

export function setUnauthorizedHandler(handler: (() => void) | undefined) {
  unauthorizedHandler = handler
}

export const api = createApiClient(API_URL, {
  getAccessToken: () => getStoredSession()?.accessToken ?? null,
  onUnauthorized: () => unauthorizedHandler?.(),
})
