import { useState } from 'react'
import {
  Activity,
  CheckCircle2,
  ChevronRight,
  Clock,
  ExternalLink,
  FileSearch,
  Gauge,
  Globe,
  Play,
  RefreshCw,
} from 'lucide-react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'

import { isActiveAudit, type Audit } from '@/api/audits'
import { getErrorMessage } from '@/api/errors'
import { useCreateAudit, useWebsiteAudits } from '@/api/queries/audits'
import { useWebsites } from '@/api/queries/websites'
import type { Website } from '@/api/websites'
import { formatDateTime, hostOf, relativeTime, scoreTone } from '@/lib/format'
import { useToast } from '@/lib/toast'
import { useWorkspace } from '@/lib/workspace'

import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { DownloadReportButton } from '@/components/ui/DownloadReportButton'
import { EmptyState } from '@/components/ui/EmptyState'
import { IconTile } from '@/components/ui/IconTile'
import { siteTint } from '@/components/ui/tints'
import { NextStepBanner } from '@/components/ui/NextStepBanner'
import { PageHeader } from '@/components/ui/PageHeader'
import { Progress } from '@/components/ui/Progress'
import { ScoreRing } from '@/components/ui/ScoreRing'
import { Skeleton, TableSkeleton } from '@/components/ui/Skeleton'
import { StatTile } from '@/components/ui/StatTile'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { WorkflowRail } from '@/components/ui/WorkflowRail'
import { cn } from '@/lib/cn'

const NO_WEBSITES: Website[] = []
const NO_AUDITS: Audit[] = []

export function AuditsPage() {
  const { projectId } = useWorkspace()
  const { pushToast } = useToast()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()

  const websitesQuery = useWebsites(projectId)
  const websites = websitesQuery.data ?? NO_WEBSITES
  const fromQuery = Number(searchParams.get('websiteId'))
  const selectedWebsiteId = websites.some((site) => site.id === fromQuery)
    ? fromQuery
    : (websites[0]?.id ?? null)

  const auditsQuery = useWebsiteAudits(projectId, selectedWebsiteId)
  const audits = auditsQuery.data ?? NO_AUDITS
  const createAudit = useCreateAudit(projectId)
  const [createError, setCreateError] = useState<string | null>(null)

  const loadingWebsites = websitesQuery.isPending
  const loadingAudits = selectedWebsiteId !== null && auditsQuery.isPending
  const creatingAudit = createAudit.isPending
  const error =
    createError ??
    (websitesQuery.isError
      ? 'Unable to load websites. Please try again.'
      : auditsQuery.isError
        ? 'Unable to load audits. Please try again.'
        : null)

  function setSelectedWebsiteId(websiteId: number) {
    const next = new URLSearchParams(searchParams)
    next.set('websiteId', String(websiteId))
    setSearchParams(next, { replace: true })
  }

  function handleCreateAudit() {
    if (selectedWebsiteId === null) return
    setCreateError(null)
    createAudit.mutate(selectedWebsiteId, {
      onSuccess: (audit) => {
        pushToast({
          tone: 'success',
          title: 'Audit started',
          description: `Opening live report for audit #${audit.id}.`,
        })
        navigate(`/audits/${audit.id}`)
      },
      onError: (err) => {
        setCreateError(getErrorMessage(err, 'Unable to start the audit. Please try again.'))
      },
    })
  }

  function retry() {
    setCreateError(null)
    void websitesQuery.refetch()
    if (selectedWebsiteId !== null) void auditsQuery.refetch()
  }

  const selectedWebsite = websites.find((w) => w.id === selectedWebsiteId)
  const hasActive = audits.some((audit) => isActiveAudit(audit.status))
  const completed = audits.filter((audit) => audit.status === 'COMPLETED')
  const latest = completed[0] ?? null
  const finished = audits.filter((audit) => !isActiveAudit(audit.status))
  const successRate = finished.length ? Math.round((completed.length / finished.length) * 100) : null
  const phase =
    websites.length === 0
      ? 'connect'
      : hasActive
        ? audits.find((a) => a.status === 'ANALYZING')
          ? 'analyze'
          : 'crawl'
        : completed.length > 0
          ? 'report'
          : 'crawl'

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Workspace"
        title="Audits"
        description="Launch crawls, watch live progress, and open completed SEO reports."
        action={
          <Button
            onClick={handleCreateAudit}
            loading={creatingAudit}
            disabled={selectedWebsiteId === null || loadingWebsites}
          >
            <Play className="h-4 w-4" />
            Run audit
          </Button>
        }
      />

      {phase !== 'report' && <WorkflowRail phase={phase} />}

      {!loadingWebsites && websites.length === 0 ? (
        <NextStepBanner
          title="Connect a website first"
          description="Audits need a property. Add one, then come back to start crawling."
          actionLabel="Add website"
          to="/websites"
          icon={<Globe className="h-4 w-4" />}
        />
      ) : !loadingAudits && audits.length === 0 && selectedWebsiteId !== null ? (
        <NextStepBanner
          title="No audits yet for this site"
          description="Run an audit to crawl pages, score SEO health, and unlock issues."
          actionLabel="Run audit now"
          icon={<Play className="h-4 w-4" />}
          onAction={() => void handleCreateAudit()}
        />
      ) : null}

      {error && (
        <Alert
          variant="error"
          title="Something went wrong"
          action={
            <Button size="sm" variant="secondary" onClick={retry}>
              <RefreshCw className="h-3.5 w-3.5" />
              Retry
            </Button>
          }
        >
          {error}
        </Alert>
      )}

      {loadingWebsites ? (
        <div className="flex gap-2">
          <Skeleton className="h-11 w-44 rounded-full" />
          <Skeleton className="h-11 w-36 rounded-full" />
        </div>
      ) : websites.length > 0 ? (
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div
            role="radiogroup"
            aria-label="Website"
            className="-mx-1 flex max-w-full gap-2 overflow-x-auto px-1 py-1"
          >
            {websites.map((site) => {
              const active = site.id === selectedWebsiteId
              const host = hostOf(site.url)
              return (
                <button
                  key={site.id}
                  type="button"
                  role="radio"
                  aria-checked={active}
                  onClick={() => setSelectedWebsiteId(site.id)}
                  className={cn(
                    'flex h-11 shrink-0 items-center gap-2.5 rounded-full py-1 pr-4 pl-1.5 text-sm font-semibold transition-all',
                    active
                      ? 'bg-surface text-main card-shadow ring-2 ring-accent/60 dark:bg-surface-elevated'
                      : 'bg-surface-low text-muted hover:text-main dark:bg-surface/70',
                  )}
                >
                  <IconTile tint={siteTint(site.id)} className="rounded-full">
                    {host.charAt(0)}
                  </IconTile>
                  <span className="max-w-[12rem] truncate">{site.name || host}</span>
                </button>
              )
            })}
          </div>
          {selectedWebsite && (
            <a
              href={selectedWebsite.url}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1.5 text-[13px] font-semibold text-accent transition-opacity hover:opacity-75"
            >
              Open website
              <ExternalLink className="h-3.5 w-3.5" />
            </a>
          )}
        </div>
      ) : null}

      {selectedWebsite && audits.length > 0 && (
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          <StatTile
            tint="pink"
            icon={<Gauge />}
            label="Latest score"
            value={latest?.score ?? '—'}
            valueClassName={scoreTone(latest?.score ?? null)}
            caption={latest ? `Audit #${latest.id}` : 'No completed audit'}
            to={latest ? `/audits/${latest.id}` : undefined}
          />
          <StatTile
            tint="green"
            icon={<Activity />}
            label="Audits run"
            value={audits.length}
            caption={hasActive ? 'One is running now' : 'Last 12 shown'}
          />
          <StatTile
            tint="blue"
            icon={<CheckCircle2 />}
            label="Success rate"
            value={successRate === null ? '—' : `${successRate}%`}
            caption={`${completed.length} completed`}
          />
          <StatTile
            tint="purple"
            icon={<Clock />}
            label="Last run"
            value={relativeTime(audits[0].createdAt)}
            valueClassName="text-[24px]"
            caption={formatDateTime(audits[0].createdAt)}
          />
        </div>
      )}

      <section className="widget">
        <div className="flex items-end justify-between gap-3 px-6 pt-5 pb-3">
          <div>
            <h2 className="text-headline text-main">Audit history</h2>
            <p className="mt-0.5 text-xs text-dim">
              {selectedWebsite ? hostOf(selectedWebsite.url) : 'Previous SEO audits'}
              {audits.length > 0 && ` · ${audits.length} audit${audits.length === 1 ? '' : 's'}`}
            </p>
          </div>
        </div>

        {loadingWebsites || loadingAudits ? (
          <TableSkeleton rows={4} />
        ) : websites.length === 0 ? (
          <EmptyState
            icon={<Globe className="h-6 w-6" />}
            title="No websites to audit"
            description="Connect a website first, then return here to run audits."
            action={
              <Link to="/websites">
                <Button size="sm">Go to websites</Button>
              </Link>
            }
          />
        ) : audits.length === 0 ? (
          <EmptyState
            icon={<FileSearch className="h-6 w-6" />}
            title="No audits yet"
            description="Run your first audit to analyze this website."
            action={
              <Button
                onClick={handleCreateAudit}
                loading={creatingAudit}
                disabled={selectedWebsiteId === null}
              >
                <Play className="h-4 w-4" />
                Run first audit
              </Button>
            }
          />
        ) : (
          <ul className="px-3 pb-3">
            {audits.map((audit) => (
              <AuditRow key={audit.id} audit={audit} projectId={projectId} />
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}

function AuditRow({ audit, projectId }: { audit: Audit; projectId: number }) {
  const inProgress = isActiveAudit(audit.status)
  const progress =
    audit.pagesCrawled === 0 ? 0 : Math.min(100, Math.round((audit.pagesAnalyzed / audit.pagesCrawled) * 100))

  return (
    <li className="group flex items-center gap-4 rounded-2xl px-3 py-3 transition-colors hover:bg-surface-elevated/50">
      <Link to={`/audits/${audit.id}`} className="flex min-w-0 flex-1 items-center gap-4">
        {audit.status === 'COMPLETED' ? (
          <ScoreRing score={audit.score} size={46} strokeWidth={5} />
        ) : (
          <span
            className={cn(
              'flex h-[46px] w-[46px] shrink-0 items-center justify-center rounded-full',
              inProgress ? 'bg-accent-surface text-accent' : 'bg-surface-elevated text-dim',
            )}
          >
            {inProgress ? <Activity className="h-5 w-5 animate-pulse" /> : <FileSearch className="h-5 w-5" />}
          </span>
        )}
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <p className="text-sm font-semibold text-main group-hover:text-accent">Audit #{audit.id}</p>
            <StatusBadge status={audit.status} />
          </div>
          <p className="mt-0.5 truncate text-xs text-dim">
            {formatDateTime(audit.createdAt)} · {audit.pagesAnalyzed}/{audit.pagesCrawled} pages analyzed
          </p>
          {inProgress && <Progress className="mt-2 max-w-xs" value={progress} tone="accent" />}
        </div>
        <span className={cn('num hidden w-12 text-right text-xl font-bold sm:block', scoreTone(audit.score))}>
          {audit.score ?? '—'}
        </span>
      </Link>
      {!inProgress && (
        <DownloadReportButton projectId={projectId} auditId={audit.id} className="hidden md:inline-flex" />
      )}
      <Link
        to={`/audits/${audit.id}`}
        aria-label={`Open audit ${audit.id}`}
        className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-dim transition-colors hover:bg-surface-elevated hover:text-main"
      >
        <ChevronRight className="h-4 w-4" />
      </Link>
    </li>
  )
}
