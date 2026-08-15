import { ApiError, getApiErrorMessage } from '@shared/api/api'

export function getAuthErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    switch (error.status) {
      case 400:
        return 'Check the entered details and try again.'
      case 401:
        return 'Invalid email or password.'
      case 409:
        return 'This email or nickname is already in use.'
      default:
        return getApiErrorMessage(error)
    }
  }

  return getApiErrorMessage(error)
}
