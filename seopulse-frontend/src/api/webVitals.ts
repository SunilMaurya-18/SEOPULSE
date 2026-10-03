import api from './axios'

export type WebVitalsState = 'PENDING' | 'READY' | 'FAILED' | 'UNAVAILABLE'

export interface WebVitals {
  state: WebVitalsState
  strategy: string | null
  url: string | null
  performanceScore: number | null
  lab: {
    lcpMs: number | null
    cls: number | null
    tbtMs: number | null
    fcpMs: number | null
    speedIndexMs: number | null
  } | null
  /** 75th percentile of real Chrome users; only for sites with enough traffic. */
  field: {
    lcpMs: number | null
    cls: number | null
    inpMs: number | null
    category: string | null
  } | null
  errorMessage: string | null
  measuredAt: string | null
}

export const webVitalsApi = {
  get: (projectId: number, auditId: number) =>
    api.get<WebVitals>(`/projects/${projectId}/audits/${auditId}/web-vitals`).then((response) => response.data),
}
