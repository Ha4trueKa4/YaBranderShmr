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
    return schema.parse(response)
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
