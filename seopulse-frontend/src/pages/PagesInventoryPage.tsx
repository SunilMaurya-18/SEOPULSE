import { Fragment, useEffect, useState } from 'react'
import { ChevronDown, ChevronRight, FileSearch } from 'lucide-react'
import { Link } from 'react-router-dom'

import { websiteApi, type Website } from '@/api/websites'
import { auditApi, type Audit, type AuditPage } from '@/api/audits'
import { useWorkspace } from '@/lib/workspace'

import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { PageHeader } from '@/components/ui/PageHeader'
import { Select } from '@/components/ui/Select'
import { Badge } from '@/components/ui/Badge'
import { StatusBadge } from '@/components/ui/StatusBadge'
import {
  Table,
  THead,
  TBody,
  TR,
  TH,
  TD,
} from '@/components/ui/Table'
import { Pagination } from '@/components/ui/Pagination'
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
  const [expanded, setExpanded] = useState<number | null>(null)
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
    if (!auditId) {
      setPages([])
      return
    }
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
    loadPages()
  }, [auditId, page, projectId])

  if (loading) return <PageSkeleton />

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Pages"
        title="URL inventory"
        description="Inspect crawled URLs, HTTP status, metadata, and on-page signals."
        action={
          auditId ? (
            <Link to={`/audits/${auditId}/pages`}>
              <Button variant="secondary" size="sm">
                Full audit pages
              </Button>
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
        <Card>
          <EmptyState
            icon={<FileSearch className="h-5 w-5" />}
            title="No crawled pages yet"
            description="Add a website and run an audit to populate the URL inventory."
            action={
              <Link to="/websites">
                <Button size="sm">Add website</Button>
              </Link>
            }
          />
        </Card>
      ) : (
        <>
          <Card padded>
            <div className="grid gap-3 sm:grid-cols-2">
              <Select
                label="Website"
                value={websiteId}
                onChange={(e) => setWebsiteId(e.target.value)}
                options={websites.map((w) => ({
                  value: String(w.id),
                  label: w.url,
                }))}
              />
              <Select
                label="Audit"
                value={auditId}
                onChange={(e) => {
                  setAuditId(e.target.value)
                  setPage(0)
                }}
                options={audits.map((a) => ({
                  value: String(a.id),
                  label: `#${a.id} · ${a.status}`,
                }))}
              />
            </div>
          </Card>

          <Card title="Crawled URLs" description="Expand a row for technical inspection details.">
            {loadingPages ? (
              <TableSkeleton />
            ) : pages.length === 0 ? (
              <EmptyState
                title="No pages in this audit"
                description="The selected audit has not produced page records yet."
              />
            ) : (
              <>
                <Table>
                  <THead>
                    <TR>
                      <TH className="w-8" />
                      <TH>URL</TH>
                      <TH>Status</TH>
                      <TH>HTTP</TH>
                      <TH>Title</TH>
                    </TR>
                  </THead>
                  <TBody>
                    {pages.map((p) => {
                      const open = expanded === p.id
                      return (
                        <Fragment key={p.id}>
                          <TR>
                            <TD>
                              <button
                                type="button"
                                className="rounded p-1 text-dim hover:bg-surface-elevated hover:text-main"
                                onClick={() =>
                                  setExpanded(open ? null : p.id)
                                }
                                aria-expanded={open}
                                aria-label="Toggle URL details"
                              >
                                {open ? (
                                  <ChevronDown className="h-4 w-4" />
                                ) : (
                                  <ChevronRight className="h-4 w-4" />
                                )}
                              </button>
                            </TD>
                            <TD>
                              <span className="block max-w-[280px] truncate font-mono text-xs text-main sm:max-w-md">
                                {p.url}
                              </span>
                            </TD>
                            <TD>
                              <StatusBadge status={p.status} />
                            </TD>
                            <TD mono>
                              {p.statusCode ?? '—'}
                            </TD>
                            <TD>
                              <span className="line-clamp-1 text-muted">
                                {p.title || '—'}
                              </span>
                            </TD>
                          </TR>
                          {open && (
                            <TR className="hover:bg-transparent">
                              <TD colSpan={5} className="bg-surface-low">
                                <UrlInspection page={p} />
                              </TD>
                            </TR>
                          )}
                        </Fragment>
                      )
                    })}
                  </TBody>
                </Table>
                <Pagination
                  page={page}
                  totalPages={totalPages}
                  totalElements={totalElements}
                  onPageChange={setPage}
                />
              </>
            )}
          </Card>
        </>
      )}
    </div>
  )
}

function UrlInspection({ page }: { page: AuditPage }) {
  const fields = [
    { label: 'Canonical', value: page.canonicalUrl },
    { label: 'Meta description', value: page.metaDescription },
    { label: 'Content-Type', value: page.contentType },
    { label: 'Word count', value: page.wordCount?.toString() },
    { label: 'H1 count', value: String(page.h1Count) },
    { label: 'Images', value: String(page.imageCount) },
    { label: 'Images w/o alt', value: String(page.imagesWithoutAlt) },
    { label: 'Internal links', value: String(page.internalLinkCount) },
    { label: 'External links', value: String(page.externalLinkCount) },
    { label: 'Depth', value: String(page.depth) },
    { label: 'Crawled at', value: page.crawledAt },
  ]

  return (
    <div className="grid gap-3 py-2 sm:grid-cols-2 lg:grid-cols-3">
      {fields.map((f) => (
        <div key={f.label}>
          <p className="font-mono text-[10px] tracking-wider text-dim uppercase">
            {f.label}
          </p>
          <p className="mt-1 break-all font-mono text-xs text-main">
            {f.value || '—'}
          </p>
        </div>
      ))}
      <div className="sm:col-span-2 lg:col-span-3">
        <Badge variant="neutral">URL inspection</Badge>
      </div>
    </div>
  )
}
