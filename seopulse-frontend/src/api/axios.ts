import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'

import { captureError } from '@/lib/monitoring'
import type { AuthResponse } from './auth'

export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082/api/v1'

/*
 * The access token lives only in memory; the long-lived refresh token is an
 * HttpOnly cookie scoped to /api/v1/auth, so a reload bootstraps the session
 * by calling /auth/refresh.
 */
let accessToken: string | null = null

type SessionListener = (session: AuthResponse | null) => void
const sessionListeners = new Set<SessionListener>()

export function getAccessToken(): string | null {
  return accessToken
}

export function setAccessToken(token: string | null) {
  accessToken = token
}

/** Notified whenever a background refresh succeeds (session) or fails (null). */
export function onSessionChange(listener: SessionListener): () => void {
  sessionListeners.add(listener)
  return () => sessionListeners.delete(listener)
}

function notifySession(session: AuthResponse | null) {
  sessionListeners.forEach((listener) => listener(session))
}

/**
 * Client for the cookie-based auth endpoints. It never sends a bearer token:
 * an expired token on a permitAll route would still be rejected with 401.
 */
export const authClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
    'X-Requested-With': 'XMLHttpRequest',
  },
})

let refreshInFlight: Promise<AuthResponse | null> | null = null

/**
 * Exchanges the refresh cookie for a new access token. Concurrent callers
 * share one request, because the backend rotates the refresh token and treats
 * a second use of the old one as token theft.
 */
export function refreshSession(): Promise<AuthResponse | null> {
  if (!refreshInFlight) {
    refreshInFlight = authClient
      .post<AuthResponse>('/auth/refresh')
      .then((response) => {
        setAccessToken(response.data.accessToken)
        return response.data
      })
      .catch(() => {
        setAccessToken(null)
        return null
      })
      .finally(() => {
        refreshInFlight = null
      })
  }
  return refreshInFlight
}

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as RetriableConfig | undefined
    if ((error.response?.status ?? 0) >= 500) {
      captureError(error)
    }
    if (error.response?.status !== 401 || !config || config._retried) {
      return Promise.reject(error)
    }

    config._retried = true
    const session = await refreshSession()
    notifySession(session)
    if (!session) {
      return Promise.reject(error)
    }

    config.headers.Authorization = `Bearer ${session.accessToken}`
    return api(config)
  },
)

export default api
