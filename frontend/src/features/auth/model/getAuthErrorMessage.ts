import { ApiError, getApiErrorMessage } from '@shared/api/api'
import i18n from '@/i18n'

export function getAuthErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    switch (error.status) {
      case 400:
        return i18n.t('Check the entered details and try again.')
      case 401:
        return i18n.t('Invalid email or password.')
      case 409:
        return i18n.t('This email or nickname is already in use.')
      default:
        return getApiErrorMessage(error)
    }
  }

  return getApiErrorMessage(error)
}

export function getYandexAuthErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    switch (error.status) {
      case 400:
        return i18n.t('Unable to complete Yandex ID sign-in. Please try again.')
      case 409:
        return i18n.t(
          'This email is already registered. Sign in with your password.',
        )
      case 502:
        return i18n.t('Yandex ID is temporarily unavailable. Please try again.')
      default:
        return getApiErrorMessage(error)
    }
  }

  return getApiErrorMessage(error)
}
