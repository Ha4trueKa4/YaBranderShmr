import { ZodError } from 'zod'

interface ResponseSchema<Response> {
  parse: (response: unknown) => Response
}

export function parseApiResponse<Response>(
  schema: ResponseSchema<Response>,
  response: unknown,
  endpoint: string,
): Response {
  try {
    return schema.parse(normalizeUtcDateTimes(response))
  } catch (error) {
    if (error instanceof ZodError) {
      console.error('API response does not match the contract', {
        endpoint,
        issues: error.issues,
        response,
      })
    }

    throw error
  }
}

function normalizeUtcDateTimes(value: unknown): unknown {
  if (typeof value === 'string') {
    // FIXME: Remove after the API returns UTC dates with the Z suffix.
    return value.replace(
      /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d+)?)?)\+00:?00$/,
      '$1Z',
    )
  }

  if (Array.isArray(value)) {
    return value.map(normalizeUtcDateTimes)
  }

  if (isRecord(value)) {
    return Object.fromEntries(
      Object.entries(value).map(([key, item]) => [
        key,
        normalizeUtcDateTimes(item),
      ]),
    )
  }

  return value
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}
