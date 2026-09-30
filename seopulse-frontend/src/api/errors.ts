import axios from 'axios'

/** RFC 9457 problem response, as produced by the backend's GlobalExceptionHandler. */
export interface ApiProblem {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  code?: string
  requestId?: string
  timestamp?: string
  errors?: Record<string, string>
}

function getProblem(err: unknown): ApiProblem | null {
  if (!axios.isAxiosError(err)) return null
  const data = err.response?.data
  return data && typeof data === 'object' ? (data as ApiProblem) : null
}

export function getErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err) && !err.response) {
    return 'Unable to reach the server. Check your connection and try again.'
  }
  const problem = getProblem(err)
  const firstFieldError = problem?.errors ? Object.values(problem.errors)[0] : undefined
  return firstFieldError || problem?.detail || fallback
}

export function getErrorCode(err: unknown): string | undefined {
  return getProblem(err)?.code
}

export function getRequestId(err: unknown): string | undefined {
  if (!axios.isAxiosError(err)) return undefined
  const header = err.response?.headers?.['x-request-id']
  return getProblem(err)?.requestId ?? (typeof header === 'string' ? header : undefined)
}
