import { getApiErrorMessage } from '@shared/api/api'
import i18n from '@/i18n'

const contentPolicyPattern =
  /censor|moderation|content[\s_-]*(?:policy|filter)|safety[\s_-]*filter|policy[\s_-]*violation|inappropriate/i

function getErrorDetails(error: unknown): string[] {
  if (typeof error === 'string') {
    return [error]
  }

  if (!error || typeof error !== 'object') {
    return []
  }

  const response = error as Record<string, unknown>

  const details = ['message', 'detail', 'error']
    .map((key) => response[key])
    .filter((value): value is string => typeof value === 'string')

  return [...details, ...getErrorDetails(response.response)]
}

export function isContentPolicyRejection(error: unknown): boolean {
  return getErrorDetails(error).some((detail) =>
    contentPolicyPattern.test(detail),
  )
}

export function getGenerationErrorMessage(
  error: unknown,
  fallbackMessage?: string,
): string {
  if (isContentPolicyRejection(error)) {
    return i18n.t(
      'The request was rejected due to content restrictions. Please change the description and try again.',
    )
  }

  return fallbackMessage ? i18n.t(fallbackMessage) : getApiErrorMessage(error)
}
