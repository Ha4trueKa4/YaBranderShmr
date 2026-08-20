import i18n from '@/i18n'

import { ApiError } from './client.ts'

export function getApiErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    switch (error.status) {
      case 400:
        return i18n.t('The request contains invalid data. Please try again.')
      case 401:
        return i18n.t('Please sign in and try again.')
      case 403:
        return i18n.t('You do not have permission to perform this action.')
      case 404:
        return i18n.t('The requested content was not found.')
      case 409:
        return i18n.t(
          'The request conflicts with existing data. Please try again.',
        )
      default:
        return i18n.t(
          'The service is temporarily unavailable. Please try again.',
        )
    }
  }

  if (error instanceof TypeError) {
    return i18n.t('Unable to connect to the service. Please try again.')
  }

  return i18n.t('Something went wrong. Please try again.')
}
