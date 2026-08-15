import { ApiError } from './client.ts'

export function getApiErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    switch (error.status) {
      case 400:
        return 'The request contains invalid data. Please try again.'
      case 401:
        return 'Please sign in and try again.'
      case 403:
        return 'You do not have permission to perform this action.'
      case 404:
        return 'The requested content was not found.'
      case 409:
        return 'The request conflicts with existing data. Please try again.'
      default:
        return 'The service is temporarily unavailable. Please try again.'
    }
  }

  if (error instanceof TypeError) {
    return 'Unable to connect to the service. Please try again.'
  }

  return 'Something went wrong. Please try again.'
}
