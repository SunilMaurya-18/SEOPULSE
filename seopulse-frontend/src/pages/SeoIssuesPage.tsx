import { useCallback, useEffect, useMemo, useState } from 'react'
import { CheckCircle2, RefreshCw } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'

import { auditApi, type SeoIssue } from '@/api/audits'
import { useAuditSummary } from '@/api/queries/audits'
import { useAuditComparison } from '@/api/queries/insights'
import { IssueGroups } from '@/features/issues/IssueGroups'
import { severityTabs, type SeverityFilter } from '@/features/issues/severity'
import { useWorkspace } from '@/lib/workspace'

import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { PageHeader } from '@/components/ui/PageHeader'
import { Pagination } from '@/components/ui/Pagination'
import { SearchField } from '@/components/ui/PillSelect'
import { CardSkeleton } from '@/components/ui/Skeleton'
import { Tabs } from '@/components/ui/Tabs'

const PAGE_SIZE = 100

export function SeoIssuesPage() {
  const { projectId } = useWorkspace()
  const { auditId } = useParams<{ auditId: string }>()
  const summary = useAuditSummary(projectId, Number(auditId))
  const comparison = useAuditComparison(projectId, Number(auditId), summary.data?.status === 'COMPLETED')
  const newFingerprints = useMemo(
    () => (comparison.data?.baselineAuditId ? new Set(comparison.data.newFingerprints) : undefined),
    [comparison.data],
  )
  const [onlyNew, setOnlyNew] = useState(false)

  const [issues, setIssues] = useState<SeoIssue[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [severity, setSeverity] = useState<SeverityFilter>('ALL')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  const loadIssues = useCallback(async (targetPage: number) => {
    if (!auditId) {
      setError('Invalid audit ID.')
      setLoading(false)
      return
    }

    const id = Number(auditId)
    if (Number.isNaN(id)) {
      setError('Invalid audit ID.')
      setLoading(false)
      return
    }

    try {
      setLoading(true)
      setError(null)

      const response = await auditApi.getIssues(
        projectId,
        id,
        targetPage,
        PAGE_SIZE,
        severity === 'ALL' ? undefined : severity,
      )

      setIssues(response.content ?? [])
      setPage(response.page)
      setTotalPages(response.totalPages)
      setTotalElements(response.totalElements)
    } catch (err) {
      console.error('Failed to load SEO issues:', err)
      setError('Unable to load SEO issues. Please try again.')
    } finally {
      setLoading(false)
    }
  }, [auditId, projectId, severity])

  useEffect(() => {
    void loadIssues(0)
  }, [loadIssues])

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    const scoped =
      onlyNew && newFingerprints
        ? issues.filter((issue) => !!issue.fingerprint && newFingerprints.has(issue.fingerprint))
        : issues
    if (!q) return scoped

    return scoped.filter((issue) => {
      return (
        issue.message.toLowerCase().includes(q) ||
        issue.url.toLowerCase().includes(q) ||
        issue.ruleCode.toLowerCase().includes(q) ||
        (issue.ruleTitle ?? '').toLowerCase().includes(q)
      )
    })
  }, [issues, search, onlyNew, newFingerprints])

  const counts = summary.data
    ? {
        total: summary.data.totalIssues,
        errors: summary.data.errorCount,
        warnings: summary.data.warningCount,
        info: summary.data.infoCount,
      }
    : null

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow={`Audit #${auditId ?? '—'}`}
        title="SEO issues"
        description="Every problem detected during this audit, grouped by rule."
        action={
          <Link to={`/audits/${auditId}`}>
            <Button variant="secondary">Back to audit</Button>
          </Link>
        }
      />

      {error && (
        <Alert
          variant="error"
          title="Failed to load issues"
          action={
            <Button size="sm" variant="secondary" onClick={() => loadIssues(page)}>
              <RefreshCw className="h-3.5 w-3.5" />
              Retry
            </Button>
          }
        >
          {error}
        </Alert>
      )}

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <Tabs
          value={severity}
          onChange={(id) => setSeverity(id as SeverityFilter)}
          items={severityTabs(counts)}
        />
        <div className="flex items-center gap-2">
          {newFingerprints && comparison.data && comparison.data.newCount > 0 && (
            <button
              type="button"
              aria-pressed={onlyNew}
              onClick={() => setOnlyNew((value) => !value)}
              className={
                onlyNew
                  ? 'h-9 shrink-0 rounded-full bg-accent px-3.5 text-[13px] font-semibold text-on-accent'
                  : 'h-9 shrink-0 rounded-full bg-surface-elevated px-3.5 text-[13px] font-semibold text-main hover:bg-surface-high'
              }
            >
              New since last audit · {comparison.data.newCount}
            </button>
          )}
          <SearchField
            label="Search issues"
            placeholder="Filter by message, URL, or rule"
            value={search}
            onChange={setSearch}
            className="sm:w-72"
          />
        </div>
      </div>

      {summary.data?.detailsPurged && (
        <Alert variant="info" title="Details archived">
          Issue details for this audit were removed by data retention. The counts above still apply.
        </Alert>
      )}

      {loading ? (
        <div className="space-y-3">
          <CardSkeleton />
          <CardSkeleton />
        </div>
      ) : filtered.length === 0 ? (
        <div className="widget">
          <EmptyState
            icon={<CheckCircle2 className="h-6 w-6" />}
            title="No issues found"
            description={
              search.trim()
                ? 'No issues match your search on this page.'
                : severity === 'ALL'
                  ? 'This audit did not report any SEO issues.'
                  : `No ${severity.toLowerCase()} issues were found.`
            }
          />
        </div>
      ) : (
        <>
          <p className="px-1 text-xs text-dim">
            {filtered.length} finding{filtered.length === 1 ? '' : 's'}
            {search.trim() ? ` of ${issues.length}` : ''}
            {!search.trim() && totalElements > issues.length ? ` · ${totalElements} total` : ''}
          </p>
          {onlyNew && (
            <p className="px-1 text-xs text-dim">Showing new issues on this page of results only.</p>
          )}
          <IssueGroups issues={filtered} newFingerprints={newFingerprints} />
        </>
      )}

      {totalPages > 1 && (
        <div className="widget overflow-hidden">
          <Pagination
            page={page}
            totalPages={totalPages}
            totalElements={totalElements}
            onPageChange={(next) => loadIssues(next)}
            className="border-t-0"
          />
        </div>
      )}
    </div>
  )
}
