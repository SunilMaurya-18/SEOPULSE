import { useEffect, useState } from 'react'
import { FileWarning, RefreshCw } from 'lucide-react'
import { Link } from 'react-router-dom'

import { websiteApi, type Website } from '@/api/websites'
import { auditApi, type Audit, type SeoIssue } from '@/api/audits'
import { useWorkspace } from '@/lib/workspace'

import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { Input } from '@/components/ui/Input'
import { PageHeader } from '@/components/ui/PageHeader'
import { Select } from '@/components/ui/Select'
import { SeverityBadge } from '@/components/ui/StatusBadge'
import { Tabs } from '@/components/ui/Tabs'
import { PageSkeleton, TableSkeleton } from '@/components/ui/Skeleton'

type SeverityFilter = 'ALL' | 'ERROR' | 'WARNING' | 'INFO'

export function IssuesPage() {
  const { projectId } = useWorkspace()
  const [websites, setWebsites] = useState<Website[]>([])
  const [audits, setAudits] = useState<Audit[]>([])
  const [issues, setIssues] = useState<SeoIssue[]>([])
  const [websiteId, setWebsiteId] = useState<string>('')
  const [auditId, setAuditId] = useState<string>('')
  const [severity, setSeverity] = useState<SeverityFilter>('ALL')
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [loadingIssues, setLoadingIssues] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function init() {
      try {
        setLoading(true)
        setError(null)
        const res = await websiteApi.getWebsites(projectId, 0, 100)
        const list = res.content ?? []
        setWebsites(list)
        if (list.length > 0) {
          setWebsiteId(String(list[0].id))
        }
      } catch {
        setError('Unable to load websites.')
      } finally {
        setLoading(false)
      }
    }
    void init()
  }, [projectId])

  useEffect(() => {
    if (!websiteId) {
      setAudits([])
      setAuditId('')
      return
    }
    async function loadAudits() {
      try {
        const res = await auditApi.getAudits(projectId, Number(websiteId), 0, 20)
        const list = res.content ?? []
        setAudits(list)
        const preferred =
          list.find((a) => a.status === 'COMPLETED') ?? list[0]
        setAuditId(preferred ? String(preferred.id) : '')
      } catch {
        setError('Unable to load audits for this website.')
      }
    }
    void loadAudits()
  }, [websiteId, projectId])

  useEffect(() => {
    if (!auditId) {
      setIssues([])
      return
    }
    async function loadIssues() {
      try {
        setLoadingIssues(true)
        setError(null)
        const res = await auditApi.getIssues(
          projectId,
          Number(auditId),
          0,
          50,
          severity === 'ALL' ? undefined : severity,
        )
        setIssues(res.content ?? [])
      } catch {
        setError('Unable to load SEO issues.')
      } finally {
        setLoadingIssues(false)
      }
    }
    void loadIssues()
  }, [auditId, severity, projectId])

  const filtered = issues.filter((issue) => {
    if (!query.trim()) return true
    const q = query.toLowerCase()
    return (
      issue.message.toLowerCase().includes(q) ||
      issue.url.toLowerCase().includes(q) ||
      issue.ruleCode.toLowerCase().includes(q)
    )
  })

  if (loading) return <PageSkeleton />

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Issues"
        title="SEO issues"
        description="Filter and triage findings across audits by severity and rule."
        action={
          auditId ? (
            <Link to={`/audits/${auditId}`}>
              <Button variant="secondary" size="sm">
                Open audit report
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
            icon={<FileWarning className="h-5 w-5" />}
            title="No websites to inspect"
            description="Add a website and run an audit before reviewing issues."
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
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
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
                onChange={(e) => setAuditId(e.target.value)}
                options={audits.map((a) => ({
                  value: String(a.id),
                  label: `#${a.id} · ${a.status}${a.score != null ? ` · ${a.score}` : ''}`,
                }))}
                placeholder={audits.length === 0 ? 'No audits' : undefined}
              />
              <Input
                label="Search"
                placeholder="Rule, URL, or message…"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </div>
          </Card>

          <Card>
            <div className="px-4 pt-2 sm:px-5">
              <Tabs
                value={severity}
                onChange={(id) => setSeverity(id as SeverityFilter)}
                items={[
                  { id: 'ALL', label: 'All' },
                  { id: 'ERROR', label: 'Errors' },
                  { id: 'WARNING', label: 'Warnings' },
                  { id: 'INFO', label: 'Info' },
                ]}
              />
            </div>

            {loadingIssues ? (
              <TableSkeleton rows={6} />
            ) : filtered.length === 0 ? (
              <EmptyState
                icon={<FileWarning className="h-5 w-5" />}
                title="No issues match"
                description={
                  severity === 'ALL'
                    ? 'This audit has no detected issues, or none match your search.'
                    : 'Try another severity filter or clear search.'
                }
                action={
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => {
                      setQuery('')
                      setSeverity('ALL')
                    }}
                  >
                    <RefreshCw className="h-4 w-4" />
                    Reset filters
                  </Button>
                }
              />
            ) : (
              <ul className="divide-y divide-default/60">
                {filtered.map((issue) => (
                  <li key={issue.id} className="px-4 py-4 sm:px-5">
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-center gap-2">
                          <SeverityBadge severity={issue.severity} />
                          <span className="font-mono text-[11px] text-dim">
                            {issue.ruleCode}
                          </span>
                        </div>
                        <p className="mt-2 text-sm font-medium text-main">
                          {issue.message}
                        </p>
                        <p className="mt-1 truncate font-mono text-xs text-muted">
                          {issue.url}
                        </p>
                        {issue.recommendation && (
                          <p className="mt-2 text-sm leading-6 text-muted">
                            {issue.recommendation}
                          </p>
                        )}
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </>
      )}
    </div>
  )
}
