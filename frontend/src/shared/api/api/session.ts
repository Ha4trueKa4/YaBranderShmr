export interface AuthSession {
  accessToken: string
  user: {
    email: string
    id: string
    nickname: string
  }
}

const sessionStorageKey = 'brandkit:session'

function isAuthSession(value: unknown): value is AuthSession {
  if (!value || typeof value !== 'object') {
    return false
  }

  const session = value as Record<string, unknown>

  return (
    typeof session.accessToken === 'string' &&
    typeof session.user === 'object' &&
    session.user !== null
  )
}

export function getStoredSession(): AuthSession | null {
  return getSessionFromStorage(localStorage)
}

function getSessionFromStorage(storage: Storage): AuthSession | null {
  const rawSession = storage.getItem(sessionStorageKey)

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

  storage.removeItem(sessionStorageKey)

  return null
}

export function removeStoredSession() {
  localStorage.removeItem(sessionStorageKey)
}

export function setStoredSession(session: AuthSession) {
  localStorage.setItem(sessionStorageKey, JSON.stringify(session))
}
