import { api } from '../api/instance.ts'

const HEALTH_URL = import.meta.env.DEV
  ? '/api'
  : (import.meta.env.VITE_HEALTH_URL ?? 'http://158.160.155.144:8080')

export function getHealth(): Promise<string> {
  return api(HEALTH_URL)
}
