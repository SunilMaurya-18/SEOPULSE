import api from './axios'

export interface QuickCheckIssue {
  ruleCode: string
  title: string
  severity: 'ERROR' | 'WARNING' | 'INFO'
  category: string
  recommendation: string
  helpUrl: string | null
  pages: number
}

export interface QuickCheckResult {
  url: string
  score: number
  categoryScores: Record<string, number>
  pagesChecked: number
  partial: boolean
  errorCount: number
  warningCount: number
  infoCount: number
  issueTypes: number
  issues: QuickCheckIssue[]
}

export const quickCheckApi = {
  run: (url: string, captchaToken?: string) =>
    api
      .post<QuickCheckResult>('/public/quick-check', { url, ...(captchaToken ? { captchaToken } : {}) })
      .then((response) => response.data),
}
