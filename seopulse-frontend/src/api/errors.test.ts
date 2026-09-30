import { AxiosError, AxiosHeaders } from 'axios'
import { describe, expect, it } from 'vitest'

import { getErrorCode, getErrorMessage, getRequestId } from '@/api/errors'

function problemError(status: number, data: unknown, headers: Record<string, string> = {}) {
  const config = { headers: new AxiosHeaders() }
  return new AxiosError('Request failed', 'ERR_BAD_REQUEST', config, null, {
    status,
    statusText: '',
    data,
    headers,
    config,
  })
}

describe('api error helpers', () => {
  it('prefers the first field error over the generic validation detail', () => {
    const err = problemError(400, {
      detail: 'Validation failed',
      errors: { password: 'Password is too short' },
    })
    expect(getErrorMessage(err, 'fallback')).toBe('Password is too short')
  })

  it('uses the problem detail and exposes code and request id', () => {
    const err = problemError(
      403,
      { detail: 'Verify your email first', code: 'EMAIL_NOT_VERIFIED' },
      { 'x-request-id': 'req-123' },
    )
    expect(getErrorMessage(err, 'fallback')).toBe('Verify your email first')
    expect(getErrorCode(err)).toBe('EMAIL_NOT_VERIFIED')
    expect(getRequestId(err)).toBe('req-123')
  })

  it('falls back for unknown errors and reports network failures', () => {
    expect(getErrorMessage(new Error('boom'), 'fallback')).toBe('fallback')
    expect(getErrorMessage(new AxiosError('Network Error'), 'fallback')).toMatch(/reach the server/)
  })
})
