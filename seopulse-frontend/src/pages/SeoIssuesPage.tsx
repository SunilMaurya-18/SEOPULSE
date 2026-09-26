import { useEffect, useMemo, useState } from 'react'
import { Info, RefreshCw, Search } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'

import { auditApi, type SeoIssue } from '@/api/audits'
import { useWorkspace } from '@/lib/workspace'

import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { Input } from '@/components/ui/Input'
import { PageHeader } from '@/components/ui/PageHeader'
import { Pagination } from '@/components/ui/Pagination'
import { TableSkeleton } from '@/components/ui/Skeleton'
import { SeverityBadge } from '@/components/ui/StatusBadge'
import { Tabs } from '@/components/ui/Tabs'
import {
  Table,
  TBody,
  TD,
  TH,
  THead,
  TR,
  TableToolbar,
} from '@/components/ui/Table'

const PAGE_SIZE = 50

type SeverityFilter = 'ALL' | 'ERROR' | 'WARNING' | 'INFO'

export function SeoIssuesPage() {
  const { projectId } = useWorkspace()
  const { auditId } = useParams<{ auditId: string }>()

  const [issues, setIssues] = useState<SeoIssue[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [severity, setSeverity] = useState<SeverityFilter>('ALL')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  async function loadIssues(targetPage = page) {
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
  }

  useEffect(() => {
    setPage(0)
    void loadIssues(0)
  }, [auditId, severity, projectId])

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return issues

    return issues.filter((issue) => {
      return (
        issue.message.toLowerCase().includes(q) ||
        issue.url.toLowerCase().includes(q) ||
        issue.ruleCode.toLowerCase().includes(q)
      )
    })
  }, [issues, search])

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow={`Audit #${auditId ?? '—'}`}
        title="SEO Issues"
        description="Review the SEO problems detected during this audit."
        action={
          <Link to={`/audits/${auditId}`}>
            <Button variant="secondary" size="sm">
              Back to audit
            </Button>
          </Link>
        }
      />

      {error && (
        <Alert
          variant="error"
          title="Failed to load issues"
          action={
            <Button size="sm" variant="secondary" onClick={() => loadIssues()}>
              <RefreshCw className="h-3.5 w-3.5" />
              Retry
            </Button>
          }
        >
          {error}
        </Alert>
      )}

      <Card className="overflow-hidden">
        <Tabs
          className="px-4 sm:px-5"
          value={severity}
          onChange={(id) => setSeverity(id as SeverityFilter)}
          items={[
            { id: 'ALL', label: 'All' },
            { id: 'ERROR', label: 'Errors' },
            { id: 'WARNING', label: 'Warnings' },
            { id: 'INFO', label: 'Info' },
          ]}
        />

        <TableToolbar>
          <div className="relative w-full sm:max-w-sm">
            <Search className="pointer-events-none absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-dim" />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Filter by message, URL, or rule…"
              className="pl-9"
              aria-label="Search issues"
            />
          </div>
          <p className="font-mono text-[11px] text-muted">
            {filtered.length} shown
            {search.trim() ? ` of ${issues.length}` : ''}
            {!search.trim() && totalElements > 0
              ? ` · ${totalElements} total`
              : ''}
          </p>
        </TableToolbar>

        {loading ? (
          <TableSkeleton rows={6} />
        ) : filtered.length === 0 ? (
          <EmptyState
            icon={<Info className="h-5 w-5" />}
            title="No issues found"
            description={
              search.trim()
                ? 'No issues match your search on this page.'
                : severity === 'ALL'
                  ? 'This audit did not report any SEO issues.'
                  : `No ${severity.toLowerCase()} issues were found.`
            }
          />
        ) : (
          <Table>
            <THead>
              <TR className="hover:bg-transparent">
                <TH>Issue</TH>
                <TH>Severity</TH>
                <TH>Rule</TH>
                <TH>URL</TH>
                <TH>Recommendation</TH>
              </TR>
            </THead>
            <TBody>
              {filtered.map((issue) => (
                <TR key={issue.id} className="align-top">
                  <TD>
                    <p className="text-sm font-medium text-main">
                      {issue.message}
                    </p>
                  </TD>
                  <TD>
                    <SeverityBadge severity={issue.severity} />
                  </TD>
                  <TD mono>{issue.ruleCode}</TD>
                  <TD>
                    <p
                      className="max-w-[220px] truncate font-mono text-xs text-muted"
                      title={issue.url}
                    >
                      {issue.url}
                    </p>
                  </TD>
                  <TD>
                    <p className="max-w-xs text-sm leading-5 text-muted">
                      {issue.recommendation}
                    </p>
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>
        )}

        <Pagination
          page={page}
          totalPages={totalPages}
          totalElements={totalElements}
          onPageChange={(next) => loadIssues(next)}
        />
      </Card>
    </div>
  )
}
