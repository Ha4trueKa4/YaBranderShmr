type QueryValue = string | number | boolean | null | undefined

export type QueryParams = Record<string, QueryValue>

export interface ApiRequestOptions<Body = unknown>
  extends Omit<RequestInit, 'body'> {
  query?: QueryParams
  requiresAuth?: boolean
  body?: Body | FormData
}

interface ApiClientOptions {
  getAccessToken?: () => string | null
  onUnauthorized?: () => void
}

export class ApiError extends Error {
  public readonly status: number
  public readonly response: unknown

  constructor(status: number, response: unknown) {
    super(`API request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.response = response
  }
}

export function createApiClient(
  baseUrl: string,
  { getAccessToken, onUnauthorized }: ApiClientOptions = {},
) {
  return async function apiRequest<Response, Body = unknown>(
    path: string,
    options: ApiRequestOptions<Body> = {},
  ): Promise<Response> {
    const {
      query,
      body,
      headers: initialHeaders,
      requiresAuth = false,
      ...requestOptions
    } = options

    const url = new URL(
      path.startsWith('http')
        ? path
        : `${baseUrl.replace(/\/$/, '')}/${path.replace(/^\//, '')}`,
      window.location.origin,
    )

    Object.entries(query ?? {}).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        url.searchParams.set(key, String(value))
      }
    })

    const headers = new Headers(initialHeaders)
    const accessToken = getAccessToken?.()

    headers.set('Accept', 'application/json')

    if (accessToken && !headers.has('Authorization')) {
      headers.set('Authorization', `Bearer ${accessToken}`)
    }

    const isFormData =
      typeof FormData !== 'undefined' && body instanceof FormData

    if (body !== undefined && !isFormData) {
      headers.set('Content-Type', 'application/json')
    }

    const requestInit: RequestInit = {
      ...requestOptions,
      headers,
    }

    if (body !== undefined) {
      requestInit.body = isFormData ? body : JSON.stringify(body)
    }

    const response = await fetch(url, requestInit)

    const responseData = await parseResponseData(response)

    if (!response.ok) {
      if (response.status === 401 && requiresAuth && accessToken) {
        onUnauthorized?.()
      }

      throw new ApiError(response.status, responseData)
    }

    return responseData as Response
  }
}

async function parseResponseData(response: Response): Promise<unknown> {
  const contentType = response.headers.get('content-type') ?? ''

  if (contentType.includes('application/json')) {
    return response.json()
  }

  const responseText = await response.text()

  try {
    const responseData = JSON.parse(responseText)

    console.warn('API response has an unexpected Content-Type', {
      contentType,
      endpoint: response.url,
    })

    return responseData
  } catch {
    return responseText
  }
}
