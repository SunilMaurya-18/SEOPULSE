import { useQuery } from '@tanstack/react-query'

import { auditApi, isActiveAudit, type Audit } from '@/api/audits'
import { projectApi, type ProjectSummary } from '@/api/projects'
import { websiteApi, type Website } from '@/api/websites'
import { queryKeys } from './keys'

const DASHBOARD_POLL_MS = 4000

export interface DashboardData {
  summary: ProjectSummary
  websites: Website[]
  auditsBySite: Record<number, Audit[]>
}

async function fetchDashboard(projectId: number): Promise<DashboardData> {
  const websites = (await websiteApi.getWebsites(projectId, 0, 100)).content ?? []

  const summary = await projectApi.getProjectSummary(projectId).catch(
    (): ProjectSummary => ({
      id: projectId,
      name: 'Workspace',
      websiteCount: websites.length,
      auditCount: 0,
      completedAuditCount: 0,
      failedAuditCount: 0,
    }),
  )

  const auditPages = await Promise.all(
    websites.map((site) =>
      auditApi
        .getAudits(projectId, site.id, 0, 12)
        .then((page) => [site.id, page.content ?? []] as const)
        .catch(() => [site.id, [] as Audit[]] as const),
    ),
  )

  return { summary, websites, auditsBySite: Object.fromEntries(auditPages) }
}

export function useDashboard(projectId: number) {
  return useQuery({
    queryKey: queryKeys.dashboard(projectId),
    queryFn: () => fetchDashboard(projectId),
    refetchInterval: (query) => {
      const bySite = query.state.data?.auditsBySite ?? {}
      const hasActive = Object.values(bySite).some((audits) =>
        audits.some((audit) => isActiveAudit(audit.status)),
      )
      return hasActive ? DASHBOARD_POLL_MS : false
    },
  })
}
