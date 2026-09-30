import { isActiveAudit, type Audit } from './audits'
import { API_BASE_URL, getAccessToken, refreshSession } from './axios'

export interface SseMessage {
  event: string
  data: string
}

/** Splits a text/event-stream buffer into complete messages plus the unparsed tail. */
export function parseSseBuffer(buffer: string): { messages: SseMessage[]; rest: string } {
  const blocks = buffer.split(/\r?\n\r?\n/)
  const rest = blocks.pop() ?? ''
  const messages: SseMessage[] = []

  for (const block of blocks) {
    let event = 'message'
    const data: string[] = []
    for (const line of block.split(/\r?\n/)) {
      if (!line || line.startsWith(':')) continue
      const colon = line.indexOf(':')
      const field = colon === -1 ? line : line.slice(0, colon)
      const value = colon === -1 ? '' : line.slice(colon + 1).replace(/^ /, '')
      if (field === 'event') event = value
      else if (field === 'data') data.push(value)
    }
    if (data.length > 0) messages.push({ event, data: data.join('\n') })
  }

  return { messages, rest }
}

export interface AuditStreamHandlers {
  onAudit: (audit: Audit) => void
  /** `finished` once a terminal status arrived; `dropped` if the stream failed or ended early. */
  onClose: (reason: 'finished' | 'dropped') => void
}

/**
 * Streams audit status changes. Uses fetch rather than EventSource because
 * EventSource cannot send an Authorization header. Returns an unsubscribe fn.
 */
export function streamAuditEvents(
  projectId: number,
  auditId: number,
  handlers: AuditStreamHandlers,
): () => void {
  const controller = new AbortController()
  const url = `${API_BASE_URL}/projects/${projectId}/audits/${auditId}/events`

  async function open(allowRefresh: boolean): Promise<Response> {
    const token = getAccessToken()
    const response = await fetch(url, {
      headers: {
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      cache: 'no-store',
      signal: controller.signal,
    })
    if (response.status === 401 && allowRefresh && (await refreshSession())) {
      return open(false)
    }
    return response
  }

  async function run() {
    let finished = false
    try {
      const response = await open(true)
      if (!response.ok || !response.body) {
        throw new Error(`Audit event stream failed with HTTP ${response.status}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      for (;;) {
        const { value, done } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const parsed = parseSseBuffer(buffer)
        buffer = parsed.rest
        for (const message of parsed.messages) {
          if (message.event !== 'audit') continue
          const audit = JSON.parse(message.data) as Audit
          handlers.onAudit(audit)
          if (!isActiveAudit(audit.status)) finished = true
        }
      }
    } catch {
      // Treated as a dropped stream below.
    }
    if (!controller.signal.aborted) {
      handlers.onClose(finished ? 'finished' : 'dropped')
    }
  }

  void run()
  return () => controller.abort()
}
