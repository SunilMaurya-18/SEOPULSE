import { useCallback, useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { auditApi, isActiveAudit, type Audit } from '@/api/audits'
import { streamAuditEvents } from '@/api/auditEvents'
import { queryKeys } from './keys'

const FALLBACK_POLL_MS = 3000
/** Safety net while the event stream is healthy, in case an event is missed. */
const LIVE_POLL_MS = 30_000
const LIST_POLL_MS = 3500

export function useAuditSummary(projectId: number, auditId: number) {
  return useQuery({
    queryKey: queryKeys.auditSummary(projectId, auditId),
    queryFn: () => auditApi.getSummary(projectId, auditId),
    enabled: Number.isFinite(auditId),
    retry: false,
  })
}

export function useWebsiteAudits(projectId: number, websiteId: number | null) {
  return useQuery({
    queryKey: queryKeys.websiteAudits(projectId, websiteId),
    queryFn: async () =>
      (await auditApi.getAudits(projectId, websiteId as number, 0, 20)).content ?? [],
    enabled: websiteId !== null,
    refetchInterval: (query) =>
      query.state.data?.some((audit) => isActiveAudit(audit.status)) ? LIST_POLL_MS : false,
  })
}

function useInvalidateAuditViews(projectId: number) {
  const queryClient = useQueryClient()
  return useCallback(
    (audit: Audit) => {
      queryClient.setQueryData(queryKeys.audit(projectId, audit.id), audit)
      void queryClient.invalidateQueries({ queryKey: queryKeys.auditSummary(projectId, audit.id) })
      void queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'audits'] })
      void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard(projectId) })
    },
    [queryClient, projectId],
  )
}

export function useCreateAudit(projectId: number) {
  const onChanged = useInvalidateAuditViews(projectId)
  return useMutation({
    mutationFn: (websiteId: number) => auditApi.createAudit(projectId, websiteId),
    onSuccess: onChanged,
  })
}

export function useCancelAudit(projectId: number) {
  const onChanged = useInvalidateAuditViews(projectId)
  return useMutation({
    mutationFn: (auditId: number) => auditApi.cancelAudit(projectId, auditId),
    onSuccess: onChanged,
  })
}

/**
 * Loads an audit and, while it is active, keeps it fresh from the server-sent
 * event stream. If the stream drops, falls back to fast polling.
 */
export function useLiveAudit(projectId: number, auditId: number) {
  const onChanged = useInvalidateAuditViews(projectId)
  const [droppedFor, setDroppedFor] = useState<number | null>(null)
  const streamSupported = typeof ReadableStream !== 'undefined'
  const polling = droppedFor === auditId || !streamSupported

  const query = useQuery({
    queryKey: queryKeys.audit(projectId, auditId),
    queryFn: () => auditApi.getAudit(projectId, auditId),
    enabled: Number.isFinite(auditId),
    refetchInterval: (q) => {
      const audit = q.state.data
      if (!audit || !isActiveAudit(audit.status)) return false
      return polling ? FALLBACK_POLL_MS : LIVE_POLL_MS
    },
  })

  const active = query.data ? isActiveAudit(query.data.status) : false

  useEffect(() => {
    if (!active || polling) return
    return streamAuditEvents(projectId, auditId, {
      onAudit: onChanged,
      onClose: (reason) => {
        if (reason === 'dropped') setDroppedFor(auditId)
      },
    })
  }, [projectId, auditId, active, polling, onChanged])

  return { ...query, live: active && !polling }
}
