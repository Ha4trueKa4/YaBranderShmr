export interface AuthSession {
  accessToken: string
  userId: string
}

const sessionStorageKey = 'brandkit:session'

function isAuthSession(value: unknown): value is AuthSession {
  if (!value || typeof value !== 'object') {
    return false
  }

  const session = value as Record<string, unknown>

  return (
    typeof session.accessToken === 'string' &&
    typeof session.userId === 'string'
  )
}

export function getStoredSession(): AuthSession | null {
  const rawSession = sessionStorage.getItem(sessionStorageKey)

  if (!rawSession) {
    return null
  }

  try {
    const session: unknown = JSON.parse(rawSession)

    if (isAuthSession(session)) {
      return session
    }
  } catch {
    // Invalid storage data is cleared below.
  }

  sessionStorage.removeItem(sessionStorageKey)

  return null
}

export function removeStoredSession() {
  sessionStorage.removeItem(sessionStorageKey)
}

export function setStoredSession(session: AuthSession) {
  sessionStorage.setItem(sessionStorageKey, JSON.stringify(session))
}
