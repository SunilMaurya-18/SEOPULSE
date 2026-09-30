import * as Sentry from '@sentry/react'
import axios from 'axios'

const dsn = import.meta.env.VITE_SENTRY_DSN as string | undefined

let enabled = false

export function initMonitoring() {
  if (!dsn || enabled) return
  Sentry.init({
    dsn,
    environment: import.meta.env.MODE,
    release: import.meta.env.VITE_RELEASE as string | undefined,
    tracesSampleRate: 0,
    dataCollection: {
      userInfo: false,
      cookies: false,
      httpHeaders: false,
      httpBodies: [],
      // Verify and reset links carry single-use tokens in the query string.
      urlQueryParams: false,
    },
    beforeSend(event) {
      if (event.request?.url) {
        event.request.url = event.request.url.split('?')[0]
      }
      return event
    },
  })
  enabled = true
}

export function setMonitoringUser(userId: number | null) {
  if (!enabled) return
  Sentry.setUser(userId === null ? null : { id: String(userId) })
}

/**
 * Reports an error; for API failures the backend's X-Request-Id is attached as
 * the `request_id` tag so the event can be matched to the server log line.
 */
export function captureError(error: unknown, extra?: Record<string, unknown>) {
  if (!enabled) return
  const tags: Record<string, string> = {}
  if (axios.isAxiosError(error)) {
    const requestId = error.response?.headers?.['x-request-id']
    if (typeof requestId === 'string') tags.request_id = requestId
    if (error.response?.status) tags.http_status = String(error.response.status)
    if (error.config?.method) tags.http_method = error.config.method.toUpperCase()
  }
  Sentry.captureException(error, { tags, extra })
}
