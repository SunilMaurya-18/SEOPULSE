export const queryKeys = {
  project: (projectId: number) => ['projects', projectId] as const,
  dashboard: (projectId: number) => ['projects', projectId, 'dashboard'] as const,
  websites: (projectId: number) => ['projects', projectId, 'websites'] as const,
  websiteAudits: (projectId: number, websiteId: number | null) =>
    ['projects', projectId, 'audits', { websiteId }] as const,
  audit: (projectId: number, auditId: number) =>
    ['projects', projectId, 'audit', auditId] as const,
  auditSummary: (projectId: number, auditId: number) =>
    ['projects', projectId, 'audit', auditId, 'summary'] as const,
}
