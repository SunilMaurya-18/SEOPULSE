import { useEffect, useId, useMemo, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import {
  Activity,
  ArrowDownRight,
  ArrowUpRight,
  CheckCircle2,
  ChevronRight,
  FileStack,
  Globe2,
  Mail,
  Plus,
  RefreshCw,
  Sparkles,
} from 'lucide-react'

import type { Audit, AuditSummary } from '@/api/audits'
import { saasApi, type BillingSnapshot } from '@/api/saas'
import type { Website } from '@/api/websites'
import { useAuditSummary } from '@/api/queries/audits'
import { useDashboard } from '@/api/queries/dashboard'
import { isOnboardingOpen, useOnboarding } from '@/api/queries/onboarding'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { NextStepBanner } from '@/components/ui/NextStepBanner'
import { PageSkeleton } from '@/components/ui/Skeleton'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { WorkflowRail, resolveWorkflowPhase } from '@/components/ui/WorkflowRail'
import { useAuth } from '@/lib/auth'
import { cn } from '@/lib/cn'
import { hostOf, relativeTime } from '@/lib/format'
import { useWorkspace } from '@/lib/workspace'
import { EmailReportDialog } from './EmailReportDialog'
import { OnboardingChecklist } from './OnboardingChecklist'

const ACTIVE = new Set(['QUEUED', 'CRAWLING', 'ANALYZING'])

const RING_COLORS = {
  health: ['#ff2d55', '#ff6b8b'],
  coverage: ['#7fe22a', '#c4ff5c'],
  reliability: ['#00c7ff', '#5ef0ff'],
} as const

const SITE_TINTS = [
  'from-[#4aa8ff] to-[#0a84ff]',
  'from-[#c58cff] to-[#9f5cf0]',
  'from-[#4ee37a] to-[#28b14c]',
  'from-[#ffb340] to-[#ff9500]',
  'from-[#ff6b8b] to-[#ff2d55]',
  'from-[#5ef0ff] to-[#00a7d6]',
]

function scoreColor(score: number | null) {
  if (score === null) return 'text-dim'
  if (score >= 80) return 'text-success'
  if (score >= 60) return 'text-warning'
  return 'text-critical'
}

function scoreBar(score: number | null) {
  if (score === null) return 'bg-surface-high'
  if (score >= 80) return 'bg-success'
  if (score >= 60) return 'bg-warning'
  return 'bg-critical'
}

function greeting() {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 18) return 'Good afternoon'
  return 'Good evening'
}

export function Dashboard() {
  const { projectId, project } = useWorkspace()
  const { user } = useAuth()
  const dashboard = useDashboard(projectId)
  const onboarding = useOnboarding(projectId)
  const [billing, setBilling] = useState<BillingSnapshot | null>(null)
  const [emailing, setEmailing] = useState<Audit | null>(null)
  const [focus, setFocus] = useState<string>('all')

  useEffect(() => {
    let cancelled = false
    saasApi
      .orgs()
      .then((orgs) => (orgs[0] ? saasApi.billing(orgs[0].id) : null))
      .then((snapshot) => {
        if (!cancelled) setBilling(snapshot)
      })
      .catch(() => {
        if (!cancelled) setBilling(null)
      })
    return () => {
      cancelled = true
    }
  }, [projectId])

  const websites = useMemo(() => dashboard.data?.websites ?? [], [dashboard.data])
  const auditsBySite = useMemo(() => dashboard.data?.auditsBySite ?? {}, [dashboard.data])
  const summary = dashboard.data?.summary

  const allAudits = useMemo(
    () =>
      Object.values(auditsBySite)
        .flat()
        .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()),
    [auditsBySite],
  )

  const selectedId = websites.some((site) => String(site.id) === focus) ? focus : 'all'
  const selected = websites.find((site) => String(site.id) === selectedId) ?? null
  const scoped = selected ? (auditsBySite[selected.id] ?? []) : allAudits
  const completedScoped = scoped.filter((audit) => audit.status === 'COMPLETED')
  const latestCompleted = completedScoped[0] ?? null
  const previousCompleted = completedScoped[1] ?? null
  const activeCrawl = scoped.find((audit) => ACTIVE.has(audit.status)) ?? null
  const latestSummary = useAuditSummary(projectId, latestCompleted?.id ?? NaN)

  const trend = completedScoped
    .slice(0, 12)
    .map((audit) => audit.score)
    .filter((score): score is number => typeof score === 'number')
    .reverse()

  const siteScores = websites
    .map((site) => latestScore(auditsBySite[site.id]))
    .filter((score): score is number => typeof score === 'number')
  const overviewScore = siteScores.length
    ? Math.round(siteScores.reduce((sum, score) => sum + score, 0) / siteScores.length)
    : null
  const displayScore = selected ? (latestCompleted?.score ?? null) : overviewScore
  const delta =
    latestCompleted?.score != null && previousCompleted?.score != null
      ? latestCompleted.score - previousCompleted.score
      : null

  if (dashboard.isPending) return <PageSkeleton />
  if (dashboard.isError) {
    return (
      <Alert variant="error" title="Workspace unavailable">
        Unable to load the dashboard.
      </Alert>
    )
  }

  const websiteCount = summary?.websiteCount ?? websites.length
  const auditCount = summary?.auditCount ?? allAudits.length
  const completedCount =
    summary?.completedAuditCount ?? allAudits.filter((audit) => audit.status === 'COMPLETED').length
  const failedCount = summary?.failedAuditCount ?? allAudits.filter((audit) => audit.status === 'FAILED').length
  const successRate = auditCount ? Math.round((completedCount / auditCount) * 100) : null
  const pagesCrawled = allAudits.reduce((sum, audit) => sum + (audit.pagesCrawled ?? 0), 0)
  const coverage =
    latestSummary.data && latestSummary.data.pagesCrawled > 0
      ? Math.round((latestSummary.data.pagesAnalyzed / latestSummary.data.pagesCrawled) * 100)
      : null
  const firstName = user?.name?.split(' ')[0]
  const today = new Date().toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long' })

  const phase = resolveWorkflowPhase({
    websiteCount,
    auditCount,
    hasActiveAudit: allAudits.some((audit) => ACTIVE.has(audit.status)),
    hasCompletedAudit: completedCount > 0,
    activeStatus: activeCrawl?.status,
  })

  return (
    <div className="space-y-8">
      <header className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[13px] font-semibold tracking-[0.02em] text-dim uppercase">{today}</p>
          <h1 className="text-large-title mt-1.5 text-main">
            {greeting()}
            {firstName ? `, ${firstName}` : ''}
          </h1>
          <p className="mt-2 flex items-center gap-2 text-[15px] text-muted">
            {activeCrawl ? (
              <>
                <span className="relative flex h-2 w-2">
                  <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-accent opacity-70" />
                  <span className="relative inline-flex h-2 w-2 rounded-full bg-accent" />
                </span>
                A crawl is running in {project.name}.
              </>
            ) : (
              <>Here&apos;s how {project.name} is performing in search.</>
            )}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <select
            value={selectedId}
            onChange={(event) => setFocus(event.target.value)}
            className="h-9 rounded-full border border-default bg-surface px-4 text-[13px] font-medium text-main card-shadow outline-none focus:ring-4 focus:ring-accent/15"
            aria-label="Focus website"
          >
            <option value="all">All websites</option>
            {websites.map((site) => (
              <option key={site.id} value={String(site.id)}>
                {site.name}
              </option>
            ))}
          </select>
          <button
            type="button"
            aria-label="Refresh dashboard"
            onClick={() => void dashboard.refetch()}
            className="flex h-9 w-9 items-center justify-center rounded-full border border-default bg-surface text-muted card-shadow transition-colors hover:text-main"
          >
            <RefreshCw className={cn('h-4 w-4', dashboard.isFetching && 'animate-spin')} />
          </button>
          <Link to="/websites">
            <Button>
              <Plus className="h-4 w-4" strokeWidth={2.5} />
              Add website
            </Button>
          </Link>
        </div>
      </header>

      {isOnboardingOpen(onboarding.data) ? (
        <div className="space-y-4">
          {activeCrawl && <WorkflowRail phase={phase} compact />}
          <OnboardingChecklist
            projectId={projectId}
            status={onboarding.data}
            firstWebsiteId={websites[0]?.id ?? null}
            activeAuditId={activeCrawl?.id ?? null}
            latestCompletedAuditId={allAudits.find((audit) => audit.status === 'COMPLETED')?.id ?? null}
          />
        </div>
      ) : phase !== 'report' && !onboarding.isPending && (
        <div className="space-y-4">
          <WorkflowRail phase={phase} />
          <NextStepBanner
            title={
              phase === 'connect'
                ? 'Connect your first website'
                : phase === 'crawl'
                  ? 'Run your first SEO audit'
                  : 'Audit in progress'
            }
            description={
              phase === 'connect'
                ? 'Add a domain to unlock crawling, scoring, and emailed reports.'
                : phase === 'crawl'
                  ? 'Start a crawl to collect pages and generate a health score.'
                  : 'Crawl telemetry is live. The report can be emailed when analysis finishes.'
            }
            actionLabel={phase === 'connect' ? 'Add website' : phase === 'crawl' ? 'Start audit' : 'Open live report'}
            to={
              phase === 'connect'
                ? '/websites'
                : phase === 'crawl'
                  ? `/audits${websites[0] ? `?websiteId=${websites[0].id}` : ''}`
                  : activeCrawl
                    ? `/audits/${activeCrawl.id}`
                    : '/audits'
            }
          />
        </div>
      )}

      <section className="grid grid-cols-12 gap-4 lg:gap-5">
        <div className="widget col-span-12 p-6 xl:col-span-7">
          <WidgetHeader
            title="Search health"
            subtitle={selected ? selected.name : 'Average across all websites'}
            action={
              latestCompleted && (
                <Link
                  to={`/audits/${latestCompleted.id}`}
                  className="inline-flex items-center gap-0.5 text-[13px] font-semibold text-accent hover:opacity-80"
                >
                  Open report <ChevronRight className="h-4 w-4" />
                </Link>
              )
            }
          />

          <div className="mt-6 grid items-center gap-8 sm:grid-cols-[auto_1fr]">
            <ActivityRings
              size={196}
              rings={[
                { value: (displayScore ?? 0) / 100, colors: RING_COLORS.health },
                { value: (coverage ?? 0) / 100, colors: RING_COLORS.coverage },
                { value: (successRate ?? 0) / 100, colors: RING_COLORS.reliability },
              ]}
              label={`SEO health score ${displayScore ?? 'pending'} out of 100`}
            />
            <dl className="space-y-4">
              <RingStat
                label="Health"
                color={RING_COLORS.health[0]}
                value={displayScore === null ? '—' : String(displayScore)}
                unit="/100"
                extra={
                  delta !== null && delta !== 0 ? (
                    <span
                      className={cn(
                        'inline-flex items-center gap-0.5 rounded-full px-2 py-0.5 text-[11px] font-semibold',
                        delta > 0 ? 'bg-success-surface text-success' : 'bg-critical-surface text-critical',
                      )}
                    >
                      {delta > 0 ? <ArrowUpRight className="h-3 w-3" /> : <ArrowDownRight className="h-3 w-3" />}
                      {delta > 0 ? '+' : ''}
                      {delta}
                    </span>
                  ) : null
                }
              />
              <RingStat
                label="Coverage"
                color={RING_COLORS.coverage[0]}
                value={coverage === null ? '—' : String(coverage)}
                unit="%"
              />
              <RingStat
                label="Reliability"
                color={RING_COLORS.reliability[0]}
                value={successRate === null ? '—' : String(successRate)}
                unit="%"
              />
            </dl>
          </div>

          <div className="mt-7 rounded-2xl bg-surface-low p-4 dark:bg-surface-elevated/40">
            <div className="flex items-center justify-between">
              <p className="text-[13px] font-semibold text-main">Score trend</p>
              <p className="text-xs text-dim">
                {trend.length > 1 ? `Last ${trend.length} audits` : 'Needs two completed audits'}
              </p>
            </div>
            <TrendChart values={trend} />
          </div>

          <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-[13px] text-muted">
              {latestCompleted
                ? `Latest report ${relativeTime(latestCompleted.createdAt).toLowerCase()} · ${hostOf(latestCompleted.websiteUrl)}`
                : 'Complete an audit to share a report.'}
            </p>
            <Button disabled={!latestCompleted} onClick={() => latestCompleted && setEmailing(latestCompleted)}>
              <Mail className="h-4 w-4" />
              Email report
            </Button>
          </div>
        </div>

        <IssueBreakdown summary={latestSummary.data ?? null} hasReport={Boolean(latestCompleted)} />

        <Kpi
          tint="from-[#4aa8ff] to-[#0a84ff]"
          icon={<Globe2 className="h-4 w-4" />}
          label="Websites"
          value={websiteCount}
          caption={billing ? `${billing.websitesUsed} of ${billing.limits.websites} on your plan` : 'Connected properties'}
          progress={billing ? billing.websitesUsed / Math.max(1, billing.limits.websites) : undefined}
        />
        <Kpi
          tint="from-[#4ee37a] to-[#28b14c]"
          icon={<Activity className="h-4 w-4" />}
          label="Audits"
          value={auditCount}
          caption={billing ? `${billing.auditsUsed} of ${billing.limits.auditsPerMonth} this month` : 'Runs in this workspace'}
          progress={billing ? billing.auditsUsed / Math.max(1, billing.limits.auditsPerMonth) : undefined}
        />
        <Kpi
          tint="from-[#5ef0ff] to-[#00a7d6]"
          icon={<CheckCircle2 className="h-4 w-4" />}
          label="Success rate"
          value={successRate === null ? '—' : `${successRate}%`}
          caption={`${completedCount} completed · ${failedCount} failed`}
        />
        <Kpi
          tint="from-[#c58cff] to-[#9f5cf0]"
          icon={<FileStack className="h-4 w-4" />}
          label="Pages crawled"
          value={pagesCrawled.toLocaleString()}
          caption="Across recent audits"
        />

        <div className="widget col-span-12 overflow-hidden xl:col-span-7">
          <div className="px-6 pt-6 pb-3">
            <WidgetHeader
              title="Websites"
              subtitle="Ranked by latest health score"
              action={
                <Link to="/websites" className="text-[13px] font-semibold text-accent hover:opacity-80">
                  Manage
                </Link>
              }
            />
          </div>
          {websites.length === 0 ? (
            <div className="flex flex-col items-center px-6 pt-6 pb-10 text-center">
              <span className="flex h-14 w-14 items-center justify-center rounded-[18px] bg-gradient-to-b from-[#4aa8ff] to-[#0a84ff] text-white shadow-[0_10px_24px_-10px_#0a84ff]">
                <Globe2 className="h-6 w-6" />
              </span>
              <p className="text-headline mt-4 text-main">No websites yet</p>
              <p className="mt-1 max-w-xs text-sm text-muted">Add a domain to start your first crawl and unlock scoring.</p>
              <Link to="/websites" className="mt-5">
                <Button size="sm">
                  <Plus className="h-4 w-4" />
                  Add website
                </Button>
              </Link>
            </div>
          ) : (
            <ul className="px-3 pb-3">
              {[...websites]
                .sort((a, b) => (latestScore(auditsBySite[b.id]) ?? -1) - (latestScore(auditsBySite[a.id]) ?? -1))
                .map((site, index) => (
                  <SiteRow
                    key={site.id}
                    site={site}
                    tint={SITE_TINTS[index % SITE_TINTS.length]}
                    audits={auditsBySite[site.id] ?? []}
                    active={String(site.id) === selectedId}
                    onFocus={() => setFocus(String(site.id) === selectedId ? 'all' : String(site.id))}
                  />
                ))}
            </ul>
          )}
        </div>

        <div className="col-span-12 flex flex-col gap-4 lg:gap-5 xl:col-span-5">
          <PlanCard billing={billing} />

          <div className="widget flex-1 overflow-hidden">
            <div className="px-6 pt-6 pb-2">
              <WidgetHeader title="Recent activity" />
            </div>
            {scoped.length === 0 ? (
              <p className="px-6 pt-2 pb-6 text-sm text-muted">No audits in this view yet.</p>
            ) : (
              <ol className="px-6 pb-5">
                {scoped.slice(0, 5).map((audit, index, list) => (
                  <li key={audit.id} className="relative flex gap-4 pb-4 last:pb-0">
                    {index < list.length - 1 && (
                      <span className="absolute top-5 bottom-0 left-[5px] w-px bg-default" aria-hidden />
                    )}
                    <span
                      className={cn(
                        'relative mt-1.5 h-[11px] w-[11px] shrink-0 rounded-full ring-4 ring-surface',
                        audit.status === 'COMPLETED'
                          ? 'bg-success'
                          : audit.status === 'FAILED'
                            ? 'bg-critical'
                            : 'bg-accent',
                      )}
                    />
                    <div className="flex min-w-0 flex-1 items-start justify-between gap-3">
                      <div className="min-w-0">
                        <Link
                          to={`/audits/${audit.id}`}
                          className="block truncate text-sm font-medium text-main hover:text-accent"
                        >
                          {hostOf(audit.websiteUrl)}
                        </Link>
                        <p className="text-xs text-dim">
                          {relativeTime(audit.createdAt)}
                          {audit.score != null && ` · Score ${audit.score}`}
                        </p>
                      </div>
                      <StatusBadge status={audit.status} />
                    </div>
                  </li>
                ))}
              </ol>
            )}
          </div>
        </div>
      </section>

      {emailing && (
        <EmailReportDialog projectId={projectId} audit={emailing} onClose={() => setEmailing(null)} />
      )}
    </div>
  )
}

function latestScore(audits: Audit[] | undefined) {
  return audits?.find((audit) => audit.status === 'COMPLETED')?.score ?? null
}

function WidgetHeader({ title, subtitle, action }: { title: string; subtitle?: string; action?: ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-3">
      <div>
        <h2 className="text-headline text-main">{title}</h2>
        {subtitle && <p className="mt-0.5 text-[13px] text-muted">{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}

function ActivityRings({
  rings,
  size,
  label,
}: {
  rings: { value: number; colors: readonly [string, string] }[]
  size: number
  label: string
}) {
  const baseId = `rings-${useId().replace(/[^a-zA-Z0-9_-]/g, '')}`
  const stroke = size * 0.1
  const gap = size * 0.018
  return (
    <svg
      width={size}
      height={size}
      viewBox={`0 0 ${size} ${size}`}
      className="mx-auto -rotate-90"
      role="img"
      aria-label={label}
    >
      <defs>
        {rings.map((ring, index) => (
          <linearGradient key={index} id={`${baseId}-${index}`} x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor={ring.colors[0]} />
            <stop offset="100%" stopColor={ring.colors[1]} />
          </linearGradient>
        ))}
      </defs>
      {rings.map((ring, index) => {
        const radius = size / 2 - stroke / 2 - index * (stroke + gap)
        const circumference = 2 * Math.PI * radius
        const value = Math.max(0, Math.min(1, ring.value))
        return (
          <g key={index}>
            <circle
              cx={size / 2}
              cy={size / 2}
              r={radius}
              fill="none"
              stroke={ring.colors[0]}
              strokeOpacity={0.18}
              strokeWidth={stroke}
            />
            <circle
              cx={size / 2}
              cy={size / 2}
              r={radius}
              fill="none"
              stroke={`url(#${baseId}-${index})`}
              strokeWidth={stroke}
              strokeLinecap="round"
              strokeDasharray={circumference}
              strokeDashoffset={circumference * (1 - value)}
              className="transition-[stroke-dashoffset] duration-1000 ease-out"
              opacity={value === 0 ? 0 : 1}
            />
          </g>
        )
      })}
    </svg>
  )
}

function RingStat({
  label,
  color,
  value,
  unit,
  extra,
}: {
  label: string
  color: string
  value: string
  unit: string
  extra?: ReactNode
}) {
  return (
    <div>
      <dt className="text-[13px] font-semibold text-main">{label}</dt>
      <dd className="mt-0.5 flex items-center gap-2">
        <span className="num text-[30px] leading-none font-bold" style={{ color }}>
          {value}
          <span className="ml-0.5 text-[15px] font-semibold opacity-80">{value === '—' ? '' : unit}</span>
        </span>
        {extra}
      </dd>
    </div>
  )
}

function TrendChart({ values }: { values: number[] }) {
  const gradientId = `trend-${useId().replace(/[^a-zA-Z0-9_-]/g, '')}`
  const width = 600
  const height = 96
  if (values.length < 2) {
    return (
      <div className="mt-3 flex h-24 items-end gap-1.5">
        {Array.from({ length: 16 }).map((_, index) => (
          <span
            key={index}
            className="flex-1 rounded-full bg-surface-elevated dark:bg-surface-high/60"
            style={{ height: `${24 + ((index * 37) % 50)}%` }}
          />
        ))}
      </div>
    )
  }
  const min = Math.min(...values, 40)
  const max = Math.max(...values, 100)
  const step = width / (values.length - 1)
  const points = values.map((value, index) => [
    index * step,
    height - ((value - min) / (max - min || 1)) * (height - 12) - 6,
  ])
  const line = points
    .map(([x, y], index) => {
      if (index === 0) return `M${x},${y}`
      const [px, py] = points[index - 1]
      const cx = (px + x) / 2
      return `C${cx},${py} ${cx},${y} ${x},${y}`
    })
    .join(' ')
  const area = `${line} L${width},${height} L0,${height} Z`
  const [lastX, lastY] = points[points.length - 1]
  return (
    <svg viewBox={`0 0 ${width} ${height}`} preserveAspectRatio="none" className="mt-3 h-24 w-full overflow-visible" role="img" aria-label="Health score trend">
      <defs>
        <linearGradient id={gradientId} x1="0" x2="0" y1="0" y2="1">
          <stop offset="0%" stopColor="#ff2d55" stopOpacity="0.32" />
          <stop offset="100%" stopColor="#ff2d55" stopOpacity="0" />
        </linearGradient>
      </defs>
      <path d={area} fill={`url(#${gradientId})`} />
      <path d={line} fill="none" stroke="#ff2d55" strokeWidth="2.5" strokeLinecap="round" vectorEffect="non-scaling-stroke" />
      <circle cx={lastX} cy={lastY} r="5" fill="#ff2d55" stroke="white" strokeWidth="2" vectorEffect="non-scaling-stroke" />
    </svg>
  )
}

function IssueBreakdown({ summary, hasReport }: { summary: AuditSummary | null; hasReport: boolean }) {
  const total = summary?.totalIssues ?? 0
  const rows = summary
    ? [
        { label: 'Errors', value: summary.errorCount, color: 'bg-critical', text: 'text-critical' },
        { label: 'Warnings', value: summary.warningCount, color: 'bg-warning', text: 'text-warning' },
        { label: 'Notices', value: summary.infoCount, color: 'bg-info', text: 'text-info' },
      ]
    : []

  return (
    <div className="widget col-span-12 flex flex-col p-6 xl:col-span-5">
      <WidgetHeader
        title="Issues"
        subtitle="From the latest completed report"
        action={
          <Link to="/issues" className="inline-flex items-center gap-0.5 text-[13px] font-semibold text-accent hover:opacity-80">
            View all <ChevronRight className="h-4 w-4" />
          </Link>
        }
      />
      {!summary ? (
        <div className="flex flex-1 flex-col items-center justify-center py-10 text-center">
          <span className="flex h-12 w-12 items-center justify-center rounded-[16px] bg-gradient-to-b from-[#ffb340] to-[#ff9500] text-white">
            <Sparkles className="h-5 w-5" />
          </span>
          <p className="mt-4 max-w-[16rem] text-sm text-muted">
            {hasReport ? 'Loading the latest report…' : 'Severity counts appear after your first completed audit.'}
          </p>
        </div>
      ) : (
        <>
          <div className="mt-6 flex items-baseline gap-2">
            <span className="num text-[44px] leading-none font-bold text-main">{total}</span>
            <span className="text-[15px] font-medium text-muted">issues found</span>
          </div>
          <div className="mt-5 flex h-3 gap-[3px] overflow-hidden rounded-full">
            {total === 0 ? (
              <div className="h-full w-full rounded-full bg-success" />
            ) : (
              rows.map((row) =>
                row.value > 0 ? (
                  <div
                    key={row.label}
                    className={cn('h-full first:rounded-l-full last:rounded-r-full', row.color)}
                    style={{ width: `${(row.value / total) * 100}%` }}
                  />
                ) : null,
              )
            )}
          </div>
          <ul className="mt-5 divide-y divide-default">
            {rows.map((row) => (
              <li key={row.label} className="flex items-center justify-between py-2.5">
                <span className="flex items-center gap-2.5 text-sm font-medium text-main">
                  <span className={cn('h-2.5 w-2.5 rounded-full', row.color)} />
                  {row.label}
                </span>
                <span className="flex items-baseline gap-2">
                  <span className={cn('num text-[17px] font-semibold', row.text)}>{row.value}</span>
                  <span className="w-10 text-right text-xs text-dim font-tabular">
                    {total ? `${Math.round((row.value / total) * 100)}%` : '0%'}
                  </span>
                </span>
              </li>
            ))}
          </ul>
          <div className="mt-auto grid grid-cols-2 gap-3 pt-5">
            <MiniStat label="Pages crawled" value={String(summary.pagesCrawled)} />
            <MiniStat label="Pages analyzed" value={String(summary.pagesAnalyzed)} />
          </div>
        </>
      )}
    </div>
  )
}

function MiniStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl bg-surface-low px-4 py-3 dark:bg-surface-elevated/40">
      <p className="text-xs font-medium text-dim">{label}</p>
      <p className="num mt-1 text-xl font-semibold text-main">{value}</p>
    </div>
  )
}

function Kpi({
  tint,
  icon,
  label,
  value,
  caption,
  progress,
}: {
  tint: string
  icon: ReactNode
  label: string
  value: number | string
  caption: string
  progress?: number
}) {
  const pct = progress === undefined ? null : Math.min(100, Math.round(progress * 100))
  return (
    <div className="widget col-span-12 p-5 transition-transform duration-300 hover:-translate-y-0.5 sm:col-span-6 xl:col-span-3">
      <div className="flex items-center gap-2.5">
        <span
          className={cn(
            'flex h-8 w-8 items-center justify-center rounded-[10px] bg-gradient-to-b text-white shadow-[inset_0_1px_0_rgb(255_255_255/0.25)]',
            tint,
          )}
        >
          {icon}
        </span>
        <p className="text-[13px] font-semibold text-muted">{label}</p>
      </div>
      <p className="num mt-4 text-[32px] leading-none font-bold text-main">{value}</p>
      <p className="mt-2 text-xs text-dim">{caption}</p>
      {pct !== null && (
        <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-surface-elevated">
          <div
            className={cn('h-full rounded-full transition-all duration-700', pct >= 90 ? 'bg-critical' : 'bg-gradient-to-r', pct < 90 && tint)}
            style={{ width: `${Math.max(pct, 3)}%` }}
          />
        </div>
      )}
    </div>
  )
}

function PlanCard({ billing }: { billing: BillingSnapshot | null }) {
  const isFree = !billing || billing.planCode === 'FREE'
  return (
    <div className="relative overflow-hidden rounded-3xl bg-[linear-gradient(135deg,#2c2c2e_0%,#1c1c1e_45%,#3a1d1b_100%)] p-6 text-white shadow-[0_24px_48px_-24px_rgb(0_0_0/0.6),inset_0_1px_0_rgb(255_255_255/0.08)]">
      <div className="pointer-events-none absolute -top-20 -right-16 h-56 w-56 rounded-full bg-[#ff5a52]/35 blur-3xl" />
      <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(115deg,transparent_35%,rgb(255_255_255/0.07)_50%,transparent_65%)]" />
      <div className="relative flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold tracking-[0.04em] text-white/55 uppercase">Your plan</p>
          <p className="mt-1 font-display text-[26px] font-bold tracking-[-0.02em]">{billing?.planName ?? 'Free'}</p>
          <p className="mt-0.5 text-xs text-white/60">
            {billing?.status === 'ACTIVE' || !billing ? 'Active' : billing.status.toLowerCase().replace(/_/g, ' ')}
            {billing?.currentPeriodEnd && ` · Renews ${new Date(billing.currentPeriodEnd).toLocaleDateString()}`}
          </p>
        </div>
        <Link
          to={isFree ? '/pricing' : '/settings#billing'}
          className="inline-flex h-8 items-center rounded-full bg-white px-3.5 text-[13px] font-semibold text-[#1d1d1f] transition hover:bg-white/90"
        >
          {isFree ? 'Upgrade' : 'Manage'}
        </Link>
      </div>
      {billing && (
        <div className="relative mt-6 space-y-3.5">
          <PlanMeter label="Websites" used={billing.websitesUsed} limit={billing.limits.websites} />
          <PlanMeter label="Audits this month" used={billing.auditsUsed} limit={billing.limits.auditsPerMonth} />
          <p className="pt-1 text-xs text-white/50">
            Up to {billing.limits.pagesPerAudit.toLocaleString()} pages per audit · {billing.limits.members} seat
            {billing.limits.members === 1 ? '' : 's'}
          </p>
        </div>
      )}
    </div>
  )
}

function PlanMeter({ label, used, limit }: { label: string; used: number; limit: number }) {
  const pct = limit <= 0 ? 0 : Math.min(100, Math.round((used / limit) * 100))
  return (
    <div>
      <div className="mb-1.5 flex justify-between text-xs">
        <span className="font-medium text-white/75">{label}</span>
        <span className="font-semibold font-tabular">
          {used}
          <span className="text-white/45"> / {limit}</span>
        </span>
      </div>
      <div className="h-1.5 overflow-hidden rounded-full bg-white/12">
        <div
          className={cn('h-full rounded-full', pct >= 90 ? 'bg-[#ff453a]' : 'bg-gradient-to-r from-[#ff8a65] to-[#ff5a52]')}
          style={{ width: `${Math.max(pct, 3)}%` }}
        />
      </div>
    </div>
  )
}

function SiteRow({
  site,
  tint,
  audits,
  active,
  onFocus,
}: {
  site: Website
  tint: string
  audits: Audit[]
  active: boolean
  onFocus: () => void
}) {
  const latest = audits.find((audit) => audit.status === 'COMPLETED') ?? null
  const running = audits.find((audit) => ACTIVE.has(audit.status)) ?? null
  const score = latest?.score ?? null
  const host = hostOf(site.url)
  return (
    <li
      className={cn(
        'group flex items-center gap-4 rounded-2xl px-3 py-3 transition-colors hover:bg-surface-elevated/60',
        active && 'bg-accent-surface hover:bg-accent-surface',
      )}
    >
      <button
        type="button"
        onClick={onFocus}
        aria-pressed={active}
        aria-label={`Focus ${site.name}`}
        className={cn(
          'flex h-10 w-10 shrink-0 items-center justify-center rounded-[12px] bg-gradient-to-b text-[15px] font-bold text-white uppercase shadow-[inset_0_1px_0_rgb(255_255_255/0.25)]',
          tint,
        )}
      >
        {host.charAt(0)}
      </button>
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <p className="truncate text-sm font-semibold text-main">{site.name}</p>
          {site.status === 'LOCKED' && (
            <span className="rounded-full bg-warning-surface px-2 py-0.5 text-[10px] font-semibold text-warning">Locked</span>
          )}
        </div>
        <p className="truncate text-xs text-dim">
          {host}
          {latest && ` · ${relativeTime(latest.createdAt)}`}
        </p>
        <div className="mt-2 h-1 w-full max-w-[240px] overflow-hidden rounded-full bg-surface-elevated">
          <div className={cn('h-full rounded-full', scoreBar(score))} style={{ width: `${score ?? 0}%` }} />
        </div>
      </div>
      {running ? (
        <StatusBadge status={running.status} />
      ) : (
        <span className={cn('num text-[22px] font-bold', scoreColor(score))}>{score ?? '—'}</span>
      )}
      <Link
        to={`/audits?websiteId=${site.id}`}
        aria-label={`Audits for ${site.name}`}
        className="flex h-7 w-7 items-center justify-center rounded-full text-dim transition-colors hover:bg-surface-elevated hover:text-main"
      >
        <ChevronRight className="h-4 w-4" />
      </Link>
    </li>
  )
}
