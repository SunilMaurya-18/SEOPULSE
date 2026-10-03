import { useState, type ReactNode } from 'react'
import {
  AlertCircle,
  AlertTriangle,
  ChevronRight,
  Clock,
  ExternalLink,
  FileStack,
  FileText,
  FileWarning,
  Info,
  Layers,
  Mail,
  RefreshCw,
  ScanSearch,
  XCircle,
} from 'lucide-react'
import { Link, useParams } from 'react-router-dom'

import { isActiveAudit } from '@/api/audits'
import { getErrorMessage } from '@/api/errors'
import {
  useAuditSummary,
  useCancelAudit,
  useLiveAudit,
} from '@/api/queries/audits'
import { useAuditComparison, useAuditWebVitals, useWebsiteTrend } from '@/api/queries/insights'
import { EmailReportDialog } from '@/features/dashboard/EmailReportDialog'
import { CategoryScores } from '@/features/insights/CategoryScores'
import { ComparisonPanel } from '@/features/insights/ComparisonPanel'
import { ReportActions } from '@/features/insights/ReportActions'
import { TrendChart } from '@/features/insights/TrendChart'
import { WebVitalsPanel } from '@/features/insights/WebVitalsPanel'
import { formatDateTime, formatDuration, hostOf } from '@/lib/format'
import { useToast } from '@/lib/toast'
import { useWorkspace } from '@/lib/workspace'

import { Alert } from '@/components/ui/Alert'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { CrawlProgress } from '@/components/ui/CrawlProgress'
import { DownloadReportButton } from '@/components/ui/DownloadReportButton'
import { EmptyState } from '@/components/ui/EmptyState'
import { IconTile } from '@/components/ui/IconTile'
import type { Tint } from '@/components/ui/tints'
import { PageSkeleton } from '@/components/ui/Skeleton'
import { ScoreRing } from '@/components/ui/ScoreRing'
import { StatTile } from '@/components/ui/StatTile'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { cn } from '@/lib/cn'

export function AuditDetailPage() {
  const { projectId } = useWorkspace()
  const { pushToast } = useToast()
  const params = useParams<{ auditId: string }>()
  const auditId = Number(params.auditId)
  const validId = Number.isInteger(auditId) && auditId > 0
  const [emailing, setEmailing] = useState(false)

  const auditQuery = useLiveAudit(projectId, auditId)
  const summaryQuery = useAuditSummary(projectId, auditId)
  const audit = auditQuery.data ?? null
  const summary = summaryQuery.data ?? null
  const isActive = audit ? isActiveAudit(audit.status) : false
  const completed = audit?.status === 'COMPLETED'
  const comparisonQuery = useAuditComparison(projectId, auditId, completed)
  const trendQuery = useWebsiteTrend(projectId, completed ? audit?.websiteId : null)
  const webVitalsQuery = useAuditWebVitals(projectId, auditId, completed)

  const cancelAudit = useCancelAudit(projectId)

  function reload() {
    void auditQuery.refetch()
    void summaryQuery.refetch()
  }

  function handleCancel() {
    cancelAudit.mutate(auditId, {
      onSuccess: () =>
        pushToast({ tone: 'info', title: 'Audit cancelled' }),
      onError: (err) =>
        pushToast({
          tone: 'error',
          title: 'Could not cancel audit',
          description: getErrorMessage(err, 'Please try again.'),
        }),
    })
  }

  if (validId && auditQuery.isPending) return <PageSkeleton />

  if (!validId || auditQuery.isError || !audit) {
    const error = !validId
      ? 'Invalid audit ID.'
      : getErrorMessage(auditQuery.error, 'Unable to load this audit. Please try again.')
    return (
      <div className="widget">
        <EmptyState
          icon={<FileWarning className="h-6 w-6" />}
          title="Audit unavailable"
          description={error}
          action={
            <div className="flex gap-2">
              <Link to="/audits">
                <Button variant="secondary">All audits</Button>
              </Link>
              <Button onClick={reload}>
                <RefreshCw className="h-4 w-4" />
                Try again
              </Button>
            </div>
          }
        />
      </div>
    )
  }

  const score = summary?.score ?? audit.score ?? null
  const errorCount = summary?.errorCount ?? 0
  const warningCount = summary?.warningCount ?? 0
  const infoCount = summary?.infoCount ?? 0
  const totalIssues = summary?.totalIssues ?? 0
  const host = hostOf(audit.websiteUrl)
  const issuesPath = `/audits/${audit.id}/issues`

  return (
    <div className="space-y-8">
      <section className="widget relative p-6 sm:p-8">
        <div className="pointer-events-none absolute inset-0 overflow-hidden rounded-[inherit]">
          <div className="absolute -top-32 -right-24 h-80 w-80 rounded-full bg-accent/15 blur-3xl" />
        </div>
        <div className="relative flex flex-col gap-8 lg:flex-row lg:items-center">
          <ScoreRing score={score} size={164} strokeWidth={13} className="mx-auto shrink-0 lg:mx-0" />

          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <p className="text-[13px] font-semibold text-accent">Audit #{audit.id}</p>
              <StatusBadge status={audit.status} />
              {audit.triggeredBy === 'SCHEDULED' && <Badge variant="accent">Scheduled</Badge>}
            </div>
            <h1 className="text-large-title mt-1.5 truncate text-main">{host}</h1>
            <a
              href={audit.websiteUrl}
              target="_blank"
              rel="noreferrer"
              className="mt-1 inline-flex max-w-full items-center gap-1.5 text-[15px] text-muted transition-colors hover:text-accent"
            >
              <span className="truncate">{audit.websiteUrl}</span>
              <ExternalLink className="h-3.5 w-3.5 shrink-0" />
            </a>

            <dl className="mt-5 flex flex-wrap gap-2">
              <MetaChip label="Started" value={formatDateTime(audit.startedAt ?? audit.createdAt)} />
              <MetaChip label="Completed" value={formatDateTime(audit.completedAt)} />
              <MetaChip label="Duration" value={formatDuration(audit.startedAt, audit.completedAt)} />
            </dl>

            <div className="mt-6 flex flex-wrap items-center gap-2">
              <DownloadReportButton
                projectId={projectId}
                auditId={audit.id}
                disabled={isActive}
                variant="primary"
                size="md"
              />
              {completed && (
                <Button variant="secondary" onClick={() => setEmailing(true)}>
                  <Mail className="h-4 w-4" />
                  Email report
                </Button>
              )}
              {completed && <ReportActions projectId={projectId} auditId={audit.id} />}
              {isActive && (
                <Button
                  variant="secondary"
                  loading={cancelAudit.isPending}
                  onClick={handleCancel}
                >
                  <XCircle className="h-4 w-4" />
                  Cancel audit
                </Button>
              )}
              <button
                type="button"
                onClick={reload}
                aria-label="Refresh"
                title="Refresh"
                className="flex h-9 w-9 items-center justify-center rounded-full bg-surface-elevated text-muted transition-colors hover:bg-surface-high hover:text-main"
              >
                <RefreshCw className="h-4 w-4" />
              </button>
            </div>
          </div>
        </div>
      </section>

      {audit.errorMessage && (
        <Alert variant="error" title="Audit error">
          {audit.errorMessage}
        </Alert>
      )}

      {isActive && (
        <CrawlProgress
          status={audit.status}
          pagesCrawled={audit.pagesCrawled}
          pagesAnalyzed={audit.pagesAnalyzed}
          errorCount={errorCount}
          warningCount={warningCount}
          websiteUrl={audit.websiteUrl}
        />
      )}

      <section>
        <h2 className="text-headline mb-3 px-1 text-main">Findings</h2>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <SeverityTile
            to={issuesPath}
            label="Errors"
            caption="Fix these first"
            value={errorCount}
            total={totalIssues}
            tint="red"
            bar="bg-critical"
            icon={<AlertCircle />}
          />
          <SeverityTile
            to={issuesPath}
            label="Warnings"
            caption="Worth improving"
            value={warningCount}
            total={totalIssues}
            tint="orange"
            bar="bg-warning"
            icon={<AlertTriangle />}
          />
          <SeverityTile
            to={issuesPath}
            label="Notices"
            caption="Good to know"
            value={infoCount}
            total={totalIssues}
            tint="blue"
            bar="bg-info"
            icon={<Info />}
          />
        </div>
      </section>

      {summary?.detailsPurged && (
        <Alert variant="info" title="Details archived">
          Page-level details for this audit were removed by your plan&apos;s data retention. Scores and counts are kept.
        </Alert>
      )}

      {completed && comparisonQuery.data && <ComparisonPanel comparison={comparisonQuery.data} />}

      {completed && <CategoryScores scores={summary?.categoryScores} />}

      {completed && <WebVitalsPanel vitals={webVitalsQuery.data} />}

      {completed && trendQuery.data && trendQuery.data.length > 1 && (
        <section className="widget p-5 sm:p-6">
          <h2 className="text-headline text-main">Score trend</h2>
          <p className="mt-0.5 mb-4 text-xs text-dim">Last {trendQuery.data.length} completed audits of {host}</p>
          <TrendChart points={trendQuery.data} currentAuditId={audit.id} />
        </section>
      )}

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatTile tint="purple" icon={<FileStack />} label="Pages crawled" value={audit.pagesCrawled.toLocaleString()} />
        <StatTile tint="teal" icon={<ScanSearch />} label="Pages analyzed" value={audit.pagesAnalyzed.toLocaleString()} />
        <StatTile tint="coral" icon={<Layers />} label="Total issues" value={totalIssues.toLocaleString()} />
        <StatTile
          tint="indigo"
          icon={<Clock />}
          label="Duration"
          value={formatDuration(audit.startedAt, audit.completedAt)}
        />
      </div>

      <section>
        <h2 className="mb-2 px-4 text-xs font-semibold tracking-[0.04em] text-dim uppercase">Explore</h2>
        <div className="widget divide-y divide-default overflow-hidden">
          <ExploreRow
            to={issuesPath}
            tint="orange"
            icon={<FileWarning />}
            label="All issues"
            detail="Filter by severity, rule and URL"
            value={totalIssues}
          />
          <ExploreRow
            to={`/audits/${audit.id}/pages`}
            tint="purple"
            icon={<FileText />}
            label="Pages inventory"
            detail="HTTP status, metadata and on-page signals"
            value={audit.pagesCrawled}
          />
        </div>
      </section>

      {emailing && (
        <EmailReportDialog projectId={projectId} audit={audit} onClose={() => setEmailing(false)} />
      )}
    </div>
  )
}

function MetaChip({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-full bg-surface-low px-3.5 py-1.5 text-xs dark:bg-surface-elevated/60">
      <dt className="inline text-dim">{label} </dt>
      <dd className="inline font-semibold text-main font-tabular">{value}</dd>
    </div>
  )
}

function SeverityTile({
  to,
  label,
  caption,
  value,
  total,
  tint,
  bar,
  icon,
}: {
  to: string
  label: string
  caption: string
  value: number
  total: number
  tint: Tint
  bar: string
  icon: ReactNode
}) {
  const share = total > 0 ? Math.round((value / total) * 100) : 0
  return (
    <Link
      to={to}
      className="widget group block p-5 transition-transform duration-300 hover:-translate-y-0.5"
    >
      <div className="flex items-center gap-2.5">
        <IconTile tint={tint}>{icon}</IconTile>
        <div className="min-w-0 flex-1">
          <p className="text-[13px] font-semibold text-main">{label}</p>
          <p className="text-xs text-dim">{caption}</p>
        </div>
        <ChevronRight className="h-4 w-4 text-dim transition-transform group-hover:translate-x-0.5 group-hover:text-main" />
      </div>
      <div className="mt-5 flex items-end justify-between">
        <p className="num text-[34px] leading-none font-bold text-main">{value.toLocaleString()}</p>
        <p className="text-xs font-semibold text-dim font-tabular">{share}% of issues</p>
      </div>
      <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-surface-elevated">
        <div className={cn('h-full rounded-full transition-all duration-700', bar)} style={{ width: `${share}%` }} />
      </div>
    </Link>
  )
}

function ExploreRow({
  to,
  tint,
  icon,
  label,
  detail,
  value,
}: {
  to: string
  tint: Tint
  icon: ReactNode
  label: string
  detail: string
  value: number
}) {
  return (
    <Link to={to} className="group flex items-center gap-3.5 px-5 py-3.5 transition-colors hover:bg-surface-elevated/50">
      <IconTile tint={tint} size="sm" className="h-[30px] w-[30px]">
        {icon}
      </IconTile>
      <div className="min-w-0 flex-1">
        <p className="text-[15px] font-medium text-main">{label}</p>
        <p className="truncate text-xs text-dim">{detail}</p>
      </div>
      <span className="text-[15px] text-dim font-tabular">{value.toLocaleString()}</span>
      <ChevronRight className="h-4 w-4 text-dim transition-transform group-hover:translate-x-0.5" />
    </Link>
  )
}
