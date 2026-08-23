const yandexOAuthStorageKey = 'brandkit:yandex-oauth'
const yandexAuthorizeUrl = 'https://oauth.yandex.ru/authorize'

interface YandexOAuthSession {
  returnPath: string
  state: string
}

function createState() {
  const values = new Uint32Array(4)

  crypto.getRandomValues(values)

  return Array.from(values, (value) => value.toString(36)).join('')
}

function getStoredSession(): YandexOAuthSession | null {
  const rawSession = localStorage.getItem(yandexOAuthStorageKey)

  if (!rawSession) {
    return null
  }

  try {
    const session: unknown = JSON.parse(rawSession)

    if (
      session &&
      typeof session === 'object' &&
      typeof (session as YandexOAuthSession).state === 'string' &&
      typeof (session as YandexOAuthSession).returnPath === 'string'
    ) {
      return session as YandexOAuthSession
    }
  } catch {
    // Invalid storage data is cleared below.
  }

  localStorage.removeItem(yandexOAuthStorageKey)

  return null
}

export function getYandexOAuthCallback() {
  return `${window.location.origin}/auth/yandex/callback`
}

export function startYandexOAuth(returnPath: string) {
  const clientId = import.meta.env.VITE_YANDEX_OAUTH_CLIENT_ID

  if (!clientId) {
    return false
  }

  const state = createState()
  localStorage.setItem(
    yandexOAuthStorageKey,
    JSON.stringify({ returnPath, state }),
  )

  const url = new URL(yandexAuthorizeUrl)
  url.searchParams.set('response_type', 'code')
  url.searchParams.set('client_id', clientId)
  url.searchParams.set('redirect_uri', getYandexOAuthCallback())
  url.searchParams.set('scope', 'login:email')
  url.searchParams.set('state', state)

  window.location.assign(url)

  return true
}

export function validateYandexOAuthState(state: string | null) {
  const session = getStoredSession()

  if (!session || !state || session.state !== state) {
    return false
  }

  return true
}

export function getYandexOAuthReturnPath() {
  const session = getStoredSession()
  localStorage.removeItem(yandexOAuthStorageKey)

  return session?.returnPath ?? '/'
}
