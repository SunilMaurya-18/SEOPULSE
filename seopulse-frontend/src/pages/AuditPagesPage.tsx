import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  CheckCircle2,
  FileStack,
  FileText,
  Heading1,
  ImageOff,
  RefreshCw,
  ServerCrash,
} from 'lucide-react'
import { Link, useParams } from 'react-router-dom'

import { auditApi, type AuditPage } from '@/api/audits'
import { PageList } from '@/features/pages/PageList'
import { useWorkspace } from '@/lib/workspace'

import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { PageHeader } from '@/components/ui/PageHeader'
import { Pagination } from '@/components/ui/Pagination'
import { TableSkeleton } from '@/components/ui/Skeleton'
import { StatTile } from '@/components/ui/StatTile'

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

  const auditNumber = auditId ? Number(auditId) : Number.NaN
  const validAuditId =
    Number.isInteger(auditNumber) && auditNumber > 0

  const loadPages = useCallback(async (targetPage: number) => {
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
    } catch (err) {
      console.error('Failed to load audit pages:', err)
      setError('Unable to load audit pages. Please try again.')
    } finally {
      setLoading(false)
    }
  }, [validAuditId, auditNumber, projectId])

  useEffect(() => {
    void loadPages(0)
  }, [loadPages])

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

        if (auditPage.status !== 'CRAWLED') {
          return stats
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
    <div className="space-y-8">
      <PageHeader
        eyebrow={`Audit #${auditId}`}
        title="Crawled pages"
        description="Every page discovered during this audit and the SEO signals collected for it."
        action={
          <>
            <Link to={`/audits/${auditId}`}>
              <Button variant="secondary">Back to audit</Button>
            </Link>
            <button
              type="button"
              onClick={() => loadPages(page)}
              aria-label="Refresh"
              title="Refresh"
              className="flex h-9 w-9 items-center justify-center rounded-full bg-surface-elevated text-muted transition-colors hover:bg-surface-high hover:text-main"
            >
              <RefreshCw className={loading ? 'h-4 w-4 animate-spin' : 'h-4 w-4'} />
            </button>
          </>
        }
      />

      <section>
        <p className="mb-3 px-1 text-xs text-dim">
          Signals for the {pages.length} page{pages.length === 1 ? '' : 's'} shown of {totalElements.toLocaleString()}
        </p>
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
          <StatTile tint="purple" icon={<FileStack />} label="In view" value={pages.length} />
          <StatTile tint="green" icon={<CheckCircle2 />} label="2xx responses" value={pageStats.success} />
          <StatTile tint="red" icon={<ServerCrash />} label="4xx / 5xx" value={pageStats.errors} />
          <StatTile tint="orange" icon={<ImageOff />} label="Alt issues" value={pageStats.altIssues} />
          <StatTile tint="blue" icon={<Heading1 />} label="H1 issues" value={pageStats.h1Issues} />
        </div>
      </section>

      {error && (
        <Alert
          variant="error"
          title="Failed to load pages"
          action={
            <Button size="sm" variant="secondary" onClick={() => loadPages(page)}>
              <RefreshCw className="h-3.5 w-3.5" />
              Retry
            </Button>
          }
        >
          {error}
        </Alert>
      )}

      <section className="widget overflow-hidden">
        <div className="px-6 pt-5 pb-3">
          <h2 className="text-headline text-main">Page inventory</h2>
          <p className="mt-0.5 text-xs text-dim">
            {totalElements.toLocaleString()} page{totalElements === 1 ? '' : 's'} discovered · tap a row to inspect
          </p>
        </div>
        {loading && pages.length === 0 ? (
          <TableSkeleton rows={8} />
        ) : pages.length === 0 ? (
          <EmptyState
            icon={<FileText className="h-6 w-6" />}
            title="No crawled pages"
            description="This audit has not produced any page records yet. If the audit is still running, try refreshing in a moment."
          />
        ) : (
          <>
            <PageList key={page} pages={pages} />
            <Pagination
              page={page}
              totalPages={totalPages}
              totalElements={totalElements}
              onPageChange={(next) => loadPages(next)}
            />
          </>
        )}
      </section>
    </div>
  )
}
