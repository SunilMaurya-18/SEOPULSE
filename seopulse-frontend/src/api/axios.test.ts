import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'

import api, { getAccessToken, onSessionChange, setAccessToken } from '@/api/axios'
import { authResponse } from '@/test/fixtures'
import { api as url, server } from '@/test/server'

function protectedEndpoint() {
  return http.get(url('/projects'), ({ request }) =>
    request.headers.get('Authorization') === 'Bearer fresh-token'
      ? HttpResponse.json({ ok: true })
      : new HttpResponse(null, { status: 401 }),
  )
}

describe('api client silent refresh', () => {
  it('refreshes once for concurrent 401s and retries every queued request', async () => {
    let refreshCalls = 0
    const refreshHeaders: Headers[] = []
    server.use(
      protectedEndpoint(),
      http.post(url('/auth/refresh'), async ({ request }) => {
        refreshCalls += 1
        refreshHeaders.push(request.headers)
        await new Promise((resolve) => setTimeout(resolve, 20))
        return HttpResponse.json(authResponse({ accessToken: 'fresh-token' }))
      }),
    )
    setAccessToken('expired-token')

    const results = await Promise.all([
      api.get('/projects'),
      api.get('/projects'),
      api.get('/projects'),
    ])

    expect(results.map((r) => r.data)).toEqual([{ ok: true }, { ok: true }, { ok: true }])
    expect(refreshCalls).toBe(1)
    expect(getAccessToken()).toBe('fresh-token')
    expect(refreshHeaders[0].get('X-Requested-With')).toBe('XMLHttpRequest')
    expect(refreshHeaders[0].get('Authorization')).toBeNull()
  })

  it('clears the session and rejects queued requests when the refresh fails', async () => {
    server.use(
      protectedEndpoint(),
      http.post(url('/auth/refresh'), () => new HttpResponse(null, { status: 401 })),
    )
    setAccessToken('expired-token')
    const listener = vi.fn()
    const unsubscribe = onSessionChange(listener)

    const outcomes = await Promise.allSettled([api.get('/projects'), api.get('/projects')])
    unsubscribe()

    expect(outcomes.every((o) => o.status === 'rejected')).toBe(true)
    expect(getAccessToken()).toBeNull()
    expect(listener).toHaveBeenCalledWith(null)
  })

  it('does not retry a request more than once', async () => {
    let calls = 0
    server.use(
      http.get(url('/projects'), () => {
        calls += 1
        return new HttpResponse(null, { status: 401 })
      }),
      http.post(url('/auth/refresh'), () =>
        HttpResponse.json(authResponse({ accessToken: 'still-rejected' })),
      ),
    )

    await expect(api.get('/projects')).rejects.toMatchObject({ response: { status: 401 } })
    expect(calls).toBe(2)
  })
})
