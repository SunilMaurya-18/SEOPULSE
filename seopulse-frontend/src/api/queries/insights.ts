import { useQuery } from '@tanstack/react-query'

import { auditApi } from '@/api/audits'
import { queryKeys } from './keys'

const REPORT_POLL_MS = 2500

export function useAuditComparison(projectId: number, auditId: number, enabled = true) {
  return useQuery({
    queryKey: queryKeys.auditComparison(projectId, auditId),
    queryFn: () => auditApi.compare(projectId, auditId),
    enabled: enabled && Number.isFinite(auditId),
    retry: false,
    staleTime: 60_000,
  })
}

export function useWebsiteTrend(projectId: number, websiteId: number | null | undefined) {
  return useQuery({
    queryKey: queryKeys.websiteTrend(projectId, websiteId ?? -1),
    queryFn: () => auditApi.trend(projectId, websiteId as number),
    enabled: typeof websiteId === 'number',
    retry: false,
    staleTime: 60_000,
  })
}

export function useAuditReports(projectId: number, auditId: number, enabled = true) {
  return useQuery({
    queryKey: queryKeys.auditReports(projectId, auditId),
    queryFn: () => auditApi.listReports(projectId, auditId),
    enabled: enabled && Number.isFinite(auditId),
    retry: false,
    refetchInterval: (query) =>
      query.state.data?.some((report) => report.status === 'PENDING' || report.status === 'GENERATING')
        ? REPORT_POLL_MS
        : false,
  })
}

export function useAuditShares(projectId: number, auditId: number, enabled = true) {
  return useQuery({
    queryKey: queryKeys.auditShares(projectId, auditId),
    queryFn: () => auditApi.listShares(projectId, auditId),
    enabled: enabled && Number.isFinite(auditId),
    retry: false,
  })
}
