import { useEffect, useState, type ReactNode } from 'react'
import {
  AlertCircle,
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  FileText,
  Info,
  RefreshCw,
} from 'lucide-react'
import { Link, useParams } from 'react-router-dom'

import {
  auditApi,
  type Audit,
  type AuditSummary,
} from '@/api/audits'
import { useWorkspace } from '@/lib/workspace'

import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { CrawlProgress } from '@/components/ui/CrawlProgress'
import { DownloadReportButton } from '@/components/ui/DownloadReportButton'
import { EmptyState } from '@/components/ui/EmptyState'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageSkeleton } from '@/components/ui/Skeleton'
import { ScoreRing } from '@/components/ui/ScoreRing'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { cn } from '@/lib/cn'

const ACTIVE_STATUSES = new Set(['QUEUED', 'CRAWLING', 'ANALYZING'])

export function AuditDetailPage() {
  const { projectId } = useWorkspace()
  const { auditId } = useParams<{ auditId: string }>()

  const [audit, setAudit] = useState<Audit | null>(null)
  const [summary, setSummary] = useState<AuditSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  async function loadAudit(silent = false) {
    if (!auditId) {
      setError('Invalid audit ID.')
      setLoading(false)
      return
    }

    try {
      if (!silent) setLoading(true)
      setError(null)

      const id = Number(auditId)
      if (Number.isNaN(id)) {
        setError('Invalid audit ID.')
        return
      }

      const [auditResponse, summaryResponse] = await Promise.all([
        auditApi.getAudit(projectId, id),
        auditApi.getSummary(projectId, id).catch(() => null),
      ])

      setAudit(auditResponse)
      if (summaryResponse) setSummary(summaryResponse)
    } catch (err) {
      console.error('Failed to load audit:', err)
      setError('Unable to load this audit. Please try again.')
    } finally {
      if (!silent) setLoading(false)
    }
  }

  useEffect(() => {
    void loadAudit()
  }, [auditId, projectId])

  useEffect(() => {
    if (!audit || !ACTIVE_STATUSES.has(audit.status)) return

    const timer = window.setInterval(() => {
      void loadAudit(true)
    }, 3000)

    return () => window.clearInterval(timer)
  }, [audit?.status, auditId, projectId])

  if (loading) return <PageSkeleton />

  if (error || !audit) {
    return (
      <div className="space-y-6">
        <Link
          to="/audits"
          className="inline-flex items-center gap-2 text-sm font-medium text-muted hover:text-main"
        >
          <ArrowLeft className="h-4 w-4" />
          Back to audits
        </Link>
        <EmptyState
          title="Audit unavailable"
          description={
            error ?? 'The requested audit could not be found.'
          }
          action={
            <Button onClick={() => void loadAudit()}>
              <RefreshCw className="h-4 w-4" />
              Try again
            </Button>
          }
        />
      </div>
    )
  }

  const score = summary?.score ?? audit.score ?? null
  const isActive = ACTIVE_STATUSES.has(audit.status)
  const errorCount = summary?.errorCount ?? 0
  const warningCount = summary?.warningCount ?? 0
  const infoCount = summary?.infoCount ?? 0
  const totalIssues = summary?.totalIssues ?? 0

  return (
    <div className="space-y-6">
      <Link
        to="/audits"
        className="inline-flex items-center gap-2 text-sm font-medium text-muted hover:text-main"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to audits
      </Link>

      <PageHeader
        eyebrow={`Audit #${audit.id}`}
        title={audit.websiteUrl}
        description="SEO audit report and analysis results."
        action={
          <>
            <StatusBadge status={audit.status} />
            <DownloadReportButton
              projectId={projectId}
              auditId={audit.id}
              disabled={isActive}
            />
            <Button
              variant="secondary"
              size="sm"
              onClick={() => void loadAudit()}
            >
              <RefreshCw className="h-3.5 w-3.5" />
              Refresh
            </Button>
          </>
        }
      />

      {/* Report meta strip */}
      <div className="flex flex-wrap gap-x-6 gap-y-2 rounded-lg border border-default bg-surface px-4 py-3 sm:px-5">
        <MetaItem label="Created" value={formatDate(audit.createdAt)} />
        <MetaItem
          label="Started"
          value={audit.startedAt ? formatDate(audit.startedAt) : '—'}
        />
        <MetaItem
          label="Completed"
          value={
            audit.completedAt ? formatDate(audit.completedAt) : '—'
          }
        />
        <MetaItem label="Website ID" value={String(audit.websiteId)} />
      </div>

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

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
        <Card className="lg:col-span-4" padded>
          <div className="flex flex-col items-center py-2">
            <ScoreRing score={score} size={128} strokeWidth={9} />
            <p className="mt-4 text-sm text-muted">SEO health score</p>
          </div>
        </Card>

        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:col-span-8 lg:grid-cols-2 xl:grid-cols-4">
          <MetricTile
            label="Pages crawled"
            value={audit.pagesCrawled}
          />
          <MetricTile
            label="Pages analyzed"
            value={audit.pagesAnalyzed}
          />
          <MetricTile label="Total issues" value={totalIssues} />
          <MetricTile
            label="Score"
            value={score}
            display={score === null ? '—' : String(score)}
          />
        </div>
      </div>

      {/* Severity summary → issues */}
      <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
        <SeverityLink
          to={`/audits/${audit.id}/issues`}
          label="Errors"
          value={errorCount}
          tone="critical"
          icon={<AlertCircle className="h-4 w-4" />}
        />
        <SeverityLink
          to={`/audits/${audit.id}/issues`}
          label="Warnings"
          value={warningCount}
          tone="warning"
          icon={<AlertTriangle className="h-4 w-4" />}
        />
        <SeverityLink
          to={`/audits/${audit.id}/issues`}
          label="Information"
          value={infoCount}
          tone="info"
          icon={<Info className="h-4 w-4" />}
        />
      </div>

      <div className="flex flex-wrap gap-3">
        <Link to={`/audits/${audit.id}/issues`}>
          <Button variant="secondary">
            View all issues
            <ArrowRight className="h-4 w-4" />
          </Button>
        </Link>
        <Link to={`/audits/${audit.id}/pages`}>
          <Button variant="secondary">
            <FileText className="h-4 w-4" />
            Pages inventory
          </Button>
        </Link>
        {!isActive && (
          <DownloadReportButton
            projectId={projectId}
            auditId={audit.id}
            size="md"
          />
        )}
      </div>
    </div>
  )
}

function MetaItem({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="font-mono text-[10px] tracking-wider text-dim uppercase">
        {label}
      </p>
      <p className="mt-0.5 font-mono text-xs text-main font-tabular">
        {value}
      </p>
    </div>
  )
}

function MetricTile({
  label,
  value,
  display,
}: {
  label: string
  value: number | null
  display?: string
}) {
  return (
    <div className="rounded-lg border border-default bg-surface p-4">
      <p className="font-mono text-[10px] tracking-wider text-dim uppercase">
        {label}
      </p>
      <p className="mt-2 font-display text-2xl font-semibold text-main font-tabular">
        {display ?? (value ?? 0).toLocaleString()}
      </p>
    </div>
  )
}

function SeverityLink({
  to,
  label,
  value,
  tone,
  icon,
}: {
  to: string
  label: string
  value: number
  tone: 'critical' | 'warning' | 'info'
  icon: ReactNode
}) {
  const tones = {
    critical: 'text-critical border-critical/20 hover:bg-critical-surface',
    warning: 'text-warning border-warning/20 hover:bg-warning-surface',
    info: 'text-info border-info/20 hover:bg-info-surface',
  }

  return (
    <Link
      to={to}
      className={cn(
        'group flex items-center justify-between rounded-lg border bg-surface p-4 transition-colors',
        tones[tone],
      )}
    >
      <div>
        <div className="flex items-center gap-2">
          {icon}
          <p className="text-sm font-medium text-main">{label}</p>
        </div>
        <p className="mt-2 font-display text-2xl font-semibold font-tabular">
          {value.toLocaleString()}
        </p>
      </div>
      <ArrowRight className="h-4 w-4 text-dim transition-transform group-hover:translate-x-0.5 group-hover:text-main" />
    </Link>
  )
}

function formatDate(date: string) {
  return new Date(date).toLocaleString()
}
