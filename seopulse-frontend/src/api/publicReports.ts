import axios from 'axios'

import { API_BASE_URL } from './axios'

export interface SharedRuleGroup {
  ruleCode: string
  title: string
  severity: string
  category: string
  count: number
  recommendation: string | null
  helpUrl: string | null
  sampleUrls: string[]
}

export interface SharedReport {
  auditId: number
  websiteName: string
  websiteUrl: string
  completedAt: string | null
  score: number | null
  scoreVersion: number | null
  categoryScores: Record<string, number>
  pagesCrawled: number
  issueCount: number
  errorCount: number
  warningCount: number
  infoCount: number
  previousScore: number | null
  newIssues: number | null
  fixedIssues: number | null
  detailsAvailable: boolean
  topIssues: SharedRuleGroup[]
  branding: {
    companyName: string | null
    brandColor: string | null
    coverText: string | null
    logoDataUrl: string | null
  } | null
  watermark: boolean
}

/** No credentials: share links work for anyone who has them. */
const publicClient = axios.create({ baseURL: API_BASE_URL })

export const publicReportApi = {
  get: (token: string) =>
    publicClient.get<SharedReport>(`/public/reports/${encodeURIComponent(token)}`).then((response) => response.data),
  pdfUrl: (token: string) => `${API_BASE_URL}/public/reports/${encodeURIComponent(token)}/pdf`,
}

/** Turns a server path such as /api/v1/public/report-files/1?... into an absolute URL on the API host. */
export function apiUrl(path: string): string {
  return new URL(path, API_BASE_URL).toString()
}
