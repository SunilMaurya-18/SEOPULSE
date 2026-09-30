import { useEffect, useState } from 'react'
import { Activity, FileSearch, Globe } from 'lucide-react'
import { Link } from 'react-router-dom'

import { websiteApi, type Website } from '@/api/websites'
import { auditApi, type Audit, type AuditPage } from '@/api/audits'
import { PageList } from '@/features/pages/PageList'
import { hostOf } from '@/lib/format'
import { useWorkspace } from '@/lib/workspace'

import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { PageHeader } from '@/components/ui/PageHeader'
import { Pagination } from '@/components/ui/Pagination'
import { PillSelect, SearchField } from '@/components/ui/PillSelect'
import { PageSkeleton, TableSkeleton } from '@/components/ui/Skeleton'

export function PagesInventoryPage() {
  const { projectId } = useWorkspace()
  const [websites, setWebsites] = useState<Website[]>([])
  const [audits, setAudits] = useState<Audit[]>([])
  const [pages, setPages] = useState<AuditPage[]>([])
  const [websiteId, setWebsiteId] = useState('')
  const [auditId, setAuditId] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [loadingPages, setLoadingPages] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function init() {
      try {
        setLoading(true)
        const res = await websiteApi.getWebsites(projectId, 0, 100)
        const list = res.content ?? []
        setWebsites(list)
        if (list[0]) setWebsiteId(String(list[0].id))
      } catch {
        setError('Unable to load websites.')
      } finally {
        setLoading(false)
      }
    }
    void init()
  }, [projectId])

  useEffect(() => {
    if (!websiteId) return
    async function loadAudits() {
      try {
        const res = await auditApi.getAudits(projectId, Number(websiteId), 0, 20)
        const list = res.content ?? []
        setAudits(list)
        const preferred = list.find((a) => a.status === 'COMPLETED') ?? list[0]
        setAuditId(preferred ? String(preferred.id) : '')
        setPage(0)
      } catch {
        setError('Unable to load audits.')
      }
    }
    void loadAudits()
  }, [websiteId, projectId])

  useEffect(() => {
    if (!auditId) return
    async function loadPages() {
      try {
        setLoadingPages(true)
        setError(null)
        const res = await auditApi.getPages(
          projectId,
          Number(auditId),
          page,
          20,
        )
        setPages(res.content ?? [])
        setTotalPages(res.totalPages ?? 0)
        setTotalElements(res.totalElements ?? 0)
      } catch {
        setError('Unable to load crawled pages.')
      } finally {
        setLoadingPages(false)
      }
    }
    void loadPages()
  }, [auditId, page, projectId])

  const auditPages = auditId ? pages : []
  const visible = query.trim()
    ? auditPages.filter((p) =>
        `${p.url} ${p.title ?? ''}`.toLowerCase().includes(query.trim().toLowerCase()),
      )
    : auditPages

  if (loading) return <PageSkeleton />

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Pages"
        title="URL inventory"
        description="Every crawled URL with its HTTP status, metadata and on-page signals."
        action={
          auditId ? (
            <Link to={`/audits/${auditId}/pages`}>
              <Button variant="secondary">Full audit pages</Button>
            </Link>
          ) : undefined
        }
      />

      {error && (
        <Alert variant="error" title="Request failed">
          {error}
        </Alert>
      )}

      {websites.length === 0 ? (
        <div className="widget">
          <EmptyState
            icon={<FileSearch className="h-6 w-6" />}
            title="No crawled pages yet"
            description="Add a website and run an audit to populate the URL inventory."
            action={
              <Link to="/websites">
                <Button size="sm">Add website</Button>
              </Link>
            }
          />
        </div>
      ) : (
        <>
          <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
            <PillSelect
              label="Website"
              icon={<Globe />}
              value={websiteId}
              onChange={(e) => setWebsiteId(e.target.value)}
              options={websites.map((w) => ({ value: String(w.id), label: w.name || hostOf(w.url) }))}
              className="lg:max-w-[260px]"
            />
            <PillSelect
              label="Audit"
              icon={<Activity />}
              value={auditId}
              onChange={(e) => {
                setAuditId(e.target.value)
                setPage(0)
              }}
              options={audits.map((a) => ({
                value: String(a.id),
                label: `#${a.id} · ${a.status.toLowerCase()}`,
              }))}
              placeholder={audits.length === 0 ? 'No audits' : undefined}
              className="lg:max-w-[240px]"
            />
            <SearchField
              label="Search pages"
              placeholder="Filter by URL or title"
              value={query}
              onChange={setQuery}
              className="lg:ml-auto lg:w-72"
            />
          </div>

          <section className="widget overflow-hidden">
            <div className="px-6 pt-5 pb-3">
              <h2 className="text-headline text-main">Crawled URLs</h2>
              <p className="mt-0.5 text-xs text-dim">
                {totalElements.toLocaleString()} page{totalElements === 1 ? '' : 's'} · tap a row to inspect
              </p>
            </div>
            {loadingPages ? (
              <TableSkeleton />
            ) : visible.length === 0 ? (
              <EmptyState
                icon={<FileSearch className="h-6 w-6" />}
                title={query.trim() ? 'No pages match' : 'No pages in this audit'}
                description={
                  query.trim()
                    ? 'Try a different URL or title.'
                    : 'The selected audit has not produced page records yet.'
                }
              />
            ) : (
              <>
                <PageList pages={visible} />
                <Pagination
                  page={page}
                  totalPages={totalPages}
                  totalElements={totalElements}
                  onPageChange={setPage}
                />
              </>
            )}
          </section>
        </>
      )}
    </div>
  )
}
