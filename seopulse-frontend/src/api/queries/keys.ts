export const queryKeys = {
  project: (projectId: number) => ['projects', projectId] as const,
  dashboard: (projectId: number) => ['projects', projectId, 'dashboard'] as const,
  onboarding: (projectId: number) => ['projects', projectId, 'onboarding'] as const,
  websites: (projectId: number) => ['projects', projectId, 'websites'] as const,
  websiteAudits: (projectId: number, websiteId: number | null) =>
    ['projects', projectId, 'audits', { websiteId }] as const,
  audit: (projectId: number, auditId: number) =>
    ['projects', projectId, 'audit', auditId] as const,
  auditSummary: (projectId: number, auditId: number) =>
    ['projects', projectId, 'audit', auditId, 'summary'] as const,
  auditComparison: (projectId: number, auditId: number) =>
    ['projects', projectId, 'audit', auditId, 'comparison'] as const,
  auditWebVitals: (projectId: number, auditId: number) =>
    ['projects', projectId, 'audit', auditId, 'web-vitals'] as const,
  auditReports: (projectId: number, auditId: number) =>
    ['projects', projectId, 'audit', auditId, 'reports'] as const,
  auditShares: (projectId: number, auditId: number) =>
    ['projects', projectId, 'audit', auditId, 'shares'] as const,
  websiteTrend: (projectId: number, websiteId: number) =>
    ['projects', projectId, 'websites', websiteId, 'trend'] as const,
  websiteSchedule: (projectId: number, websiteId: number) =>
    ['projects', projectId, 'websites', websiteId, 'schedule'] as const,
}
