import { ApiError } from '@shared/api/api/client'

export function getAuthErrorMessage(error: Error): string {
  if (error instanceof ApiError) {
    switch (error.status) {
      case 400:
        return 'Check the entered details and try again.'
      case 401:
        return 'Invalid email or password.'
      case 409:
        return 'This email or nickname is already in use.'
      default:
        return 'The service is temporarily unavailable. Please try again.'
    }
  }

  if (error instanceof TypeError) {
    return 'Unable to connect to the service. Please try again.'
  }

  return 'Something went wrong. Please try again.'
}
