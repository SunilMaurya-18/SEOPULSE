import type { ReactNode } from 'react'
import { http, HttpResponse } from 'msw'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { describe, expect, it } from 'vitest'

import { useLiveAudit } from '@/api/queries/audits'
import { audit } from '@/test/fixtures'
import { api, server } from '@/test/server'

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}

function eventStream(body: string) {
  return new HttpResponse(body, { headers: { 'Content-Type': 'text/event-stream' } })
}

describe('useLiveAudit', () => {
  it('applies server-sent updates to the cached audit', async () => {
    const completed = audit({ status: 'COMPLETED', score: 91, pagesCrawled: 5 })
    server.use(
      http.get(api('/projects/1/audits/7'), () => HttpResponse.json(audit({ status: 'CRAWLING' }))),
      http.get(api('/projects/1/audits/7/events'), () =>
        eventStream(`event: audit\ndata: ${JSON.stringify(completed)}\n\n`),
      ),
      http.get(api('/projects/1/audits/7/summary'), () => HttpResponse.json({})),
      http.get(api('/projects/1/audits'), () => HttpResponse.json({ content: [] })),
    )

    const { result } = renderHook(() => useLiveAudit(1, 7), { wrapper })

    await waitFor(() => expect(result.current.data?.status).toBe('COMPLETED'))
    expect(result.current.data?.score).toBe(91)
    expect(result.current.live).toBe(false)
  })

  it('falls back to polling when the event stream is unavailable', async () => {
    server.use(
      http.get(api('/projects/1/audits/7'), () => HttpResponse.json(audit({ status: 'CRAWLING' }))),
      http.get(api('/projects/1/audits/7/events'), () => new HttpResponse(null, { status: 503 })),
    )

    const { result } = renderHook(() => useLiveAudit(1, 7), { wrapper })

    await waitFor(() => expect(result.current.data?.status).toBe('CRAWLING'))
    await waitFor(() => expect(result.current.live).toBe(false))
  })

  it('does not open a stream for finished audits', async () => {
    server.use(
      http.get(api('/projects/1/audits/7'), () => HttpResponse.json(audit({ status: 'FAILED' }))),
    )

    const { result } = renderHook(() => useLiveAudit(1, 7), { wrapper })

    await waitFor(() => expect(result.current.data?.status).toBe('FAILED'))
    expect(result.current.live).toBe(false)
  })
})
