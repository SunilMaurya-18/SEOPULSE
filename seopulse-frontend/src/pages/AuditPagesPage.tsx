import { useEffect, useMemo, useState } from 'react'
import {
  ArrowLeft,
  ChevronDown,
  ChevronUp,
  FileText,
  RefreshCw,
} from 'lucide-react'
import { Link, useParams } from 'react-router-dom'

import { auditApi, type AuditPage } from '@/api/audits'
import { useWorkspace } from '@/lib/workspace'

import { Alert } from '@/components/ui/Alert'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { PageHeader } from '@/components/ui/PageHeader'
import { Pagination } from '@/components/ui/Pagination'
import { TableSkeleton } from '@/components/ui/Skeleton'
import { StatusBadge } from '@/components/ui/StatusBadge'
import {
  Table,
  TBody,
  TD,
  TH,
  THead,
  TR,
  TableToolbar,
} from '@/components/ui/Table'

const PAGE_SIZE = 20

export function AuditPagesPage() {
  const { projectId } = useWorkspace()
  const { auditId } = useParams<{ auditId: string }>()

  const [pages, setPages] = useState<AuditPage[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [expandedId, setExpandedId] = useState<number | null>(null)

  const auditNumber = auditId ? Number(auditId) : Number.NaN
  const validAuditId =
    Number.isInteger(auditNumber) && auditNumber > 0

  async function loadPages(targetPage = page) {
    if (!validAuditId) {
      setError('Invalid audit ID.')
      setLoading(false)
      return
    }

    try {
      setLoading(true)
      setError(null)

      const response = await auditApi.getPages(
        projectId,
        auditNumber,
        targetPage,
        PAGE_SIZE,
      )

      setPages(response.content ?? [])
      setPage(response.page)
      setTotalPages(response.totalPages)
      setTotalElements(response.totalElements)
      setExpandedId(null)
    } catch (err) {
      console.error('Failed to load audit pages:', err)
      setError('Unable to load audit pages. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    setPage(0)
    void loadPages(0)
  }, [auditId, projectId])

  const pageStats = useMemo(() => {
    return pages.reduce(
      (stats, auditPage) => {
        if (
          auditPage.statusCode !== null &&
          auditPage.statusCode >= 200 &&
          auditPage.statusCode < 300
        ) {
          stats.success += 1
        } else if (
          auditPage.statusCode !== null &&
          auditPage.statusCode >= 400
        ) {
          stats.errors += 1
        }

        if (auditPage.imagesWithoutAlt > 0) {
          stats.altIssues += 1
        }

        if (auditPage.h1Count !== 1) {
          stats.h1Issues += 1
        }

        return stats
      },
      { success: 0, errors: 0, altIssues: 0, h1Issues: 0 },
    )
  }, [pages])

  return (
    <div className="space-y-6">
      <Link
        to={`/audits/${auditId}`}
        className="inline-flex items-center gap-2 text-sm font-medium text-muted hover:text-main"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to audit
      </Link>

      <PageHeader
        eyebrow={`Audit #${auditId}`}
        title="Crawled pages"
        description="Inspect every page discovered during this audit and review the SEO signals collected by the crawler."
        action={
          <Button
            variant="secondary"
            size="sm"
            onClick={() => loadPages(page)}
            loading={loading}
          >
            <RefreshCw className="h-3.5 w-3.5" />
            Refresh
          </Button>
        }
      />

      {/* Stats for current page batch only */}
      <div>
        <p className="mb-2 font-mono text-[10px] tracking-wider text-dim uppercase">
          Stats for this page batch ({pages.length} of {totalElements}{' '}
          pages)
        </p>
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-5">
          <StatTile label="In batch" value={pages.length} />
          <StatTile label="2xx responses" value={pageStats.success} />
          <StatTile label="4xx/5xx" value={pageStats.errors} />
          <StatTile label="Alt issues" value={pageStats.altIssues} />
          <StatTile label="H1 issues" value={pageStats.h1Issues} />
        </div>
      </div>

      {error && (
        <Alert
          variant="error"
          title="Failed to load pages"
          action={
            <Button
              size="sm"
              variant="secondary"
              onClick={() => loadPages(page)}
            >
              <RefreshCw className="h-3.5 w-3.5" />
              Retry
            </Button>
          }
        >
          {error}
        </Alert>
      )}

      <Card
        title="Page inventory"
        description={`${totalElements} page${
          totalElements === 1 ? '' : 's'
        } discovered in this audit.`}
        className="overflow-hidden"
      >
        {loading && pages.length === 0 ? (
          <TableSkeleton rows={8} />
        ) : pages.length === 0 ? (
          <EmptyState
            icon={<FileText className="h-5 w-5" />}
            title="No crawled pages"
            description="This audit has not produced any page records yet. If the audit is still running, try refreshing in a moment."
          />
        ) : (
          <>
            <TableToolbar>
              <p className="font-mono text-[11px] text-muted">
                Showing page {page + 1} of {Math.max(totalPages, 1)}
              </p>
            </TableToolbar>
            <Table className="min-w-[980px]">
              <THead>
                <TR className="hover:bg-transparent">
                  <TH>Page</TH>
                  <TH>Status</TH>
                  <TH>SEO signals</TH>
                  <TH>Links</TH>
                  <TH>Depth</TH>
                  <TH className="text-right">Details</TH>
                </TR>
              </THead>
              <TBody>
                {pages.map((auditPage) => {
                  const expanded = expandedId === auditPage.id
                  return (
                    <PageRow
                      key={auditPage.id}
                      auditPage={auditPage}
                      expanded={expanded}
                      onToggle={() =>
                        setExpandedId(expanded ? null : auditPage.id)
                      }
                    />
                  )
                })}
              </TBody>
            </Table>
            <Pagination
              page={page}
              totalPages={totalPages}
              totalElements={totalElements}
              onPageChange={(next) => loadPages(next)}
            />
          </>
        )}
      </Card>
    </div>
  )
}

function PageRow({
  auditPage,
  expanded,
  onToggle,
}: {
  auditPage: AuditPage
  expanded: boolean
  onToggle: () => void
}) {
  return (
    <>
      <TR className="align-top">
        <TD>
          <div className="max-w-[420px]">
            <a
              href={auditPage.url}
              target="_blank"
              rel="noreferrer"
              className="block truncate text-sm font-medium text-main hover:text-accent"
              title={auditPage.url}
            >
              {auditPage.url}
            </a>
            <p className="mt-1 truncate text-xs text-muted">
              {auditPage.title || 'No title detected'}
            </p>
          </div>
        </TD>
        <TD>
          <div className="flex flex-col items-start gap-2">
            <StatusBadge status={auditPage.status} />
            <HttpStatus statusCode={auditPage.statusCode} />
          </div>
        </TD>
        <TD>
          <div className="flex flex-wrap gap-1.5">
            <SignalBadge
              label={`H1 ${auditPage.h1Count}`}
              problem={auditPage.h1Count !== 1}
            />
            <SignalBadge
              label={`Images ${auditPage.imageCount}`}
              problem={auditPage.imagesWithoutAlt > 0}
            />
            {auditPage.imagesWithoutAlt > 0 && (
              <SignalBadge
                label={`Alt ${auditPage.imagesWithoutAlt}`}
                problem
              />
            )}
          </div>
        </TD>
        <TD>
          <div className="font-mono text-xs text-muted font-tabular">
            {auditPage.internalLinkCount} internal
          </div>
          <div className="mt-1 font-mono text-[11px] text-dim font-tabular">
            {auditPage.externalLinkCount} external
          </div>
        </TD>
        <TD mono>{auditPage.depth}</TD>
        <TD className="text-right">
          <button
            type="button"
            onClick={onToggle}
            className="inline-flex items-center gap-1.5 rounded px-2.5 py-1.5 text-sm font-medium text-muted hover:bg-surface-elevated hover:text-main"
            aria-expanded={expanded}
          >
            {expanded ? 'Hide' : 'View'}
            {expanded ? (
              <ChevronUp className="h-4 w-4" />
            ) : (
              <ChevronDown className="h-4 w-4" />
            )}
          </button>
        </TD>
      </TR>
      {expanded && (
        <TR className="hover:bg-transparent">
          <TD colSpan={6} className="bg-surface-low !py-5">
            <PageDetails auditPage={auditPage} />
          </TD>
        </TR>
      )}
    </>
  )
}

function PageDetails({ auditPage }: { auditPage: AuditPage }) {
  return (
    <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-4">
      <DetailGroup
        title="Metadata"
        items={[
          ['Title', auditPage.title || 'Not detected'],
          [
            'Meta description',
            auditPage.metaDescription || 'Not detected',
          ],
          ['Canonical', auditPage.canonicalUrl || 'Not detected'],
          ['Content type', auditPage.contentType || 'Unknown'],
        ]}
      />
      <DetailGroup
        title="Content"
        items={[
          ['Word count', formatNumber(auditPage.wordCount)],
          ['H1 count', String(auditPage.h1Count)],
          ['Images', String(auditPage.imageCount)],
          [
            'Images without alt',
            String(auditPage.imagesWithoutAlt),
          ],
        ]}
      />
      <DetailGroup
        title="Links & crawl"
        items={[
          [
            'Internal links',
            String(auditPage.internalLinkCount),
          ],
          [
            'External links',
            String(auditPage.externalLinkCount),
          ],
          ['Depth', String(auditPage.depth)],
          [
            'HTTP status',
            auditPage.statusCode === null
              ? 'Unknown'
              : String(auditPage.statusCode),
          ],
        ]}
        monoValues
      />
      <DetailGroup
        title="Timestamps"
        items={[
          ['Crawled', formatDate(auditPage.crawledAt)],
          ['Created', formatDate(auditPage.createdAt)],
        ]}
      />
    </div>
  )
}

function DetailGroup({
  title,
  items,
  monoValues,
}: {
  title: string
  items: Array<[string, string]>
  monoValues?: boolean
}) {
  return (
    <div>
      <h3 className="font-mono text-[10px] font-medium tracking-wider text-dim uppercase">
        {title}
      </h3>
      <dl className="mt-3 space-y-3">
        {items.map(([label, value]) => (
          <div key={label}>
            <dt className="text-xs text-muted">{label}</dt>
            <dd
              className={
                monoValues
                  ? 'mt-0.5 break-words font-mono text-xs font-medium text-main font-tabular'
                  : 'mt-0.5 break-words text-sm font-medium text-main'
              }
            >
              {value}
            </dd>
          </div>
        ))}
      </dl>
    </div>
  )
}

function StatTile({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg border border-default bg-surface p-4">
      <p className="font-mono text-[10px] tracking-wider text-dim uppercase">
        {label}
      </p>
      <p className="mt-2 font-display text-2xl font-semibold text-main font-tabular">
        {value.toLocaleString()}
      </p>
    </div>
  )
}

function SignalBadge({
  label,
  problem,
}: {
  label: string
  problem: boolean
}) {
  return (
    <Badge variant={problem ? 'warning' : 'neutral'}>{label}</Badge>
  )
}

function HttpStatus({ statusCode }: { statusCode: number | null }) {
  if (statusCode === null) {
    return <Badge variant="neutral">HTTP —</Badge>
  }
  if (statusCode >= 200 && statusCode < 300) {
    return (
      <Badge variant="success">
        <span className="font-mono font-tabular">HTTP {statusCode}</span>
      </Badge>
    )
  }
  if (statusCode >= 400) {
    return (
      <Badge variant="critical">
        <span className="font-mono font-tabular">HTTP {statusCode}</span>
      </Badge>
    )
  }
  return (
    <Badge variant="warning">
      <span className="font-mono font-tabular">HTTP {statusCode}</span>
    </Badge>
  )
}

function formatNumber(value: number | null) {
  return value === null ? 'Unknown' : value.toLocaleString()
}

function formatDate(value: string | null) {
  if (!value) return '—'
  return new Date(value).toLocaleString()
}
