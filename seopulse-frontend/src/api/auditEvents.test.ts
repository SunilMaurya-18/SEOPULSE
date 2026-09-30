import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'

import { parseSseBuffer, streamAuditEvents } from '@/api/auditEvents'
import { setAccessToken } from '@/api/axios'
import { audit } from '@/test/fixtures'
import { api as url, server } from '@/test/server'

const EVENTS_PATH = '/projects/1/audits/7/events'

function sseBody(chunks: string[]) {
  const encoder = new TextEncoder()
  return new ReadableStream<Uint8Array>({
    start(controller) {
      for (const chunk of chunks) controller.enqueue(encoder.encode(chunk))
      controller.close()
    },
  })
}

function sseResponse(chunks: string[]) {
  return new HttpResponse(sseBody(chunks), {
    headers: { 'Content-Type': 'text/event-stream' },
  })
}

function waitForClose(handlers: { onClose: ReturnType<typeof vi.fn> }) {
  return vi.waitFor(() => expect(handlers.onClose).toHaveBeenCalled())
}

describe('parseSseBuffer', () => {
  it('parses named events, joins multi-line data and skips comments', () => {
    const { messages, rest } = parseSseBuffer(
      ': heartbeat\n\nevent: audit\ndata: {"a":1}\n\ndata: line1\ndata: line2\n\nevent: audit\ndata: partial',
    )

    expect(messages).toEqual([
      { event: 'audit', data: '{"a":1}' },
      { event: 'message', data: 'line1\nline2' },
    ])
    expect(rest).toBe('event: audit\ndata: partial')
  })

  it('handles CRLF line endings', () => {
    const { messages } = parseSseBuffer('event: audit\r\ndata: x\r\n\r\n')
    expect(messages).toEqual([{ event: 'audit', data: 'x' }])
  })
})

describe('streamAuditEvents', () => {
  it('delivers audit events split across chunks and reports a finished stream', async () => {
    const crawling = JSON.stringify(audit({ status: 'CRAWLING' }))
    const completed = JSON.stringify(audit({ status: 'COMPLETED', score: 88 }))
    let authorization: string | null = null
    server.use(
      http.get(url(EVENTS_PATH), ({ request }) => {
        authorization = request.headers.get('Authorization')
        return sseResponse([
          `event: audit\ndata: ${crawling}\n\n:heartbeat\n\nevent: au`,
          `dit\ndata: ${completed}\n\n`,
        ])
      }),
    )
    setAccessToken('token-1')
    const handlers = { onAudit: vi.fn(), onClose: vi.fn() }

    streamAuditEvents(1, 7, handlers)
    await waitForClose(handlers)

    expect(authorization).toBe('Bearer token-1')
    expect(handlers.onAudit.mock.calls.map(([a]) => a.status)).toEqual(['CRAWLING', 'COMPLETED'])
    expect(handlers.onClose).toHaveBeenCalledWith('finished')
  })

  it('reports a dropped stream when it ends before a terminal status', async () => {
    const crawling = JSON.stringify(audit({ status: 'CRAWLING' }))
    server.use(http.get(url(EVENTS_PATH), () => sseResponse([`event: audit\ndata: ${crawling}\n\n`])))
    const handlers = { onAudit: vi.fn(), onClose: vi.fn() }

    streamAuditEvents(1, 7, handlers)
    await waitForClose(handlers)

    expect(handlers.onClose).toHaveBeenCalledWith('dropped')
  })

  it('reports a dropped stream on an HTTP error', async () => {
    server.use(http.get(url(EVENTS_PATH), () => new HttpResponse(null, { status: 503 })))
    const handlers = { onAudit: vi.fn(), onClose: vi.fn() }

    streamAuditEvents(1, 7, handlers)
    await waitForClose(handlers)

    expect(handlers.onAudit).not.toHaveBeenCalled()
    expect(handlers.onClose).toHaveBeenCalledWith('dropped')
  })
})
