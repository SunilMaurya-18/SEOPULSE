import { useEffect, useState } from 'react'
import { Activity, FileWarning, Globe, RefreshCw } from 'lucide-react'
import { Link } from 'react-router-dom'

import { websiteApi, type Website } from '@/api/websites'
import { auditApi, type Audit, type SeoIssue } from '@/api/audits'
import { useAuditSummary } from '@/api/queries/audits'
import { IssueGroups } from '@/features/issues/IssueGroups'
import { severityTabs, type SeverityFilter } from '@/features/issues/severity'
import { hostOf } from '@/lib/format'
import { useWorkspace } from '@/lib/workspace'

import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { PageHeader } from '@/components/ui/PageHeader'
import { PillSelect, SearchField } from '@/components/ui/PillSelect'
import { Tabs } from '@/components/ui/Tabs'
import { CardSkeleton, PageSkeleton } from '@/components/ui/Skeleton'

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
  const summary = useAuditSummary(projectId, auditId ? Number(auditId) : NaN)

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
          100,
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

  const counts = summary.data
    ? {
        total: summary.data.totalIssues,
        errors: summary.data.errorCount,
        warnings: summary.data.warningCount,
        info: summary.data.infoCount,
      }
    : null

  if (loading) return <PageSkeleton />

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Issues"
        title="SEO issues"
        description="Everything the crawler flagged, grouped by rule so you can fix it once."
        action={
          auditId ? (
            <Link to={`/audits/${auditId}`}>
              <Button variant="secondary">Open audit report</Button>
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
            icon={<FileWarning className="h-6 w-6" />}
            title="No websites to inspect"
            description="Add a website and run an audit before reviewing issues."
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
              onChange={(e) => setAuditId(e.target.value)}
              options={audits.map((a) => ({
                value: String(a.id),
                label: `#${a.id}${a.score != null ? ` · Score ${a.score}` : ` · ${a.status.toLowerCase()}`}`,
              }))}
              placeholder={audits.length === 0 ? 'No audits' : undefined}
              className="lg:max-w-[240px]"
            />
            <SearchField
              label="Search issues"
              placeholder="Search rule, URL, or message"
              value={query}
              onChange={setQuery}
              className="lg:ml-auto lg:w-72"
            />
          </div>

          <Tabs
            value={severity}
            onChange={(id) => setSeverity(id as SeverityFilter)}
            items={severityTabs(counts)}
          />

          {loadingIssues ? (
            <div className="space-y-3">
              <CardSkeleton />
              <CardSkeleton />
            </div>
          ) : filtered.length === 0 ? (
            <div className="widget">
              <EmptyState
                icon={<FileWarning className="h-6 w-6" />}
                title={auditId ? 'No issues match' : 'No audits yet'}
                description={
                  !auditId
                    ? 'Run an audit for this website to see its findings.'
                    : severity === 'ALL' && !query.trim()
                      ? 'This audit has no detected issues.'
                      : 'Try another severity filter or clear the search.'
                }
                action={
                  auditId ? (
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
                  ) : (
                    <Link to={`/audits?websiteId=${websiteId}`}>
                      <Button size="sm">Go to audits</Button>
                    </Link>
                  )
                }
              />
            </div>
          ) : (
            <IssueGroups issues={filtered} />
          )}
        </>
      )}
    </div>
  )
}
