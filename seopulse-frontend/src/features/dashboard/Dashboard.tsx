import { useMemo, useState, type ReactNode } from 'react'
import {
  Activity,
  AlertTriangle,
  ArrowRight,
  CheckCircle2,
  ExternalLink,
  FileSearch,
  FileWarning,
  Globe,
  Play,
  Plus,
  RefreshCw,
  Sparkles,
} from 'lucide-react'
import { Link } from 'react-router-dom'

import type { Website } from '@/api/websites'
import type { Audit } from '@/api/audits'
import { useAuditSummary } from '@/api/queries/audits'
import { useDashboard } from '@/api/queries/dashboard'
import { useWorkspace } from '@/lib/workspace'
import { cn } from '@/lib/cn'

import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { NextStepBanner } from '@/components/ui/NextStepBanner'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageSkeleton } from '@/components/ui/Skeleton'
import { Progress } from '@/components/ui/Progress'
import { ScoreRing } from '@/components/ui/ScoreRing'
import { Select } from '@/components/ui/Select'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { CrawlProgress } from '@/components/ui/CrawlProgress'
import {
  resolveWorkflowPhase,
  WorkflowRail,
} from '@/components/ui/WorkflowRail'

const ACTIVE = new Set(['QUEUED', 'CRAWLING', 'ANALYZING'])
const ALL = 'all'

type SiteStats = {
  websiteId: number
  auditCount: number
  completedCount: number
  failedCount: number
  latestScore: number | null
  latestCompleted: Audit | null
  activeCrawl: Audit | null
  pagesCrawled: number
}

function scoreLabel(score: number | null) {
  if (score === null) return 'No score yet'
  if (score >= 80) return 'Healthy'
  if (score >= 60) return 'Needs work'
  return 'Critical'
}

function scoreTone(score: number | null) {
  if (score === null) return 'text-dim'
  if (score >= 80) return 'text-success'
  if (score >= 60) return 'text-warning'
  return 'text-critical'
}

const NO_WEBSITES: Website[] = []
const NO_AUDITS: Record<number, Audit[]> = {}

export function Dashboard() {
  const { projectId, project } = useWorkspace()
  const dashboard = useDashboard(projectId)
  const [focusedWebsiteId, setSelectedWebsiteId] = useState<string>(ALL)

  const summary = dashboard.data?.summary ?? null
  const websites = dashboard.data?.websites ?? NO_WEBSITES
  const auditsBySite = dashboard.data?.auditsBySite ?? NO_AUDITS
  const selectedWebsiteId = websites.some(
    (site) => String(site.id) === focusedWebsiteId,
  )
    ? focusedWebsiteId
    : ALL

  const allAudits = useMemo(
    () =>
      Object.values(auditsBySite)
        .flat()
        .sort(
          (a, b) =>
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
        ),
    [auditsBySite],
  )

  const siteStats = useMemo(() => {
    const map = new Map<number, SiteStats>()
    for (const site of websites) {
      const audits = auditsBySite[site.id] ?? []
      const completed = audits.filter((a) => a.status === 'COMPLETED')
      const latestCompleted = completed[0] ?? null
      map.set(site.id, {
        websiteId: site.id,
        auditCount: audits.length,
        completedCount: completed.length,
        failedCount: audits.filter((a) => a.status === 'FAILED').length,
        latestScore: latestCompleted?.score ?? null,
        latestCompleted,
        activeCrawl: audits.find((a) => ACTIVE.has(a.status)) ?? null,
        pagesCrawled: audits.reduce((sum, a) => sum + (a.pagesCrawled || 0), 0),
      })
    }
    return map
  }, [websites, auditsBySite])

  const isAll = selectedWebsiteId === ALL
  const selectedSite = websites.find(
    (site) => String(site.id) === selectedWebsiteId,
  )
  const selectedStats = selectedSite
    ? siteStats.get(selectedSite.id) ?? null
    : null

  const scopedAudits = isAll
    ? allAudits
    : selectedSite
      ? (auditsBySite[selectedSite.id] ?? [])
      : []

  const latestCompleted =
    (isAll
      ? allAudits.find((a) => a.status === 'COMPLETED')
      : selectedStats?.latestCompleted) ?? null

  const activeCrawl =
    (isAll
      ? allAudits.find((a) => ACTIVE.has(a.status))
      : selectedStats?.activeCrawl) ?? null

  const latestScore = isAll
    ? null
    : (selectedStats?.latestScore ?? null)

  const overviewScore = isAll
    ? (() => {
        const scores = websites
          .map((site) => siteStats.get(site.id)?.latestScore)
          .filter((score): score is number => typeof score === 'number')
        if (scores.length === 0) return null
        return Math.round(
          scores.reduce((sum, score) => sum + score, 0) / scores.length,
        )
      })()
    : latestScore

  const latestSummary = useAuditSummary(projectId, latestCompleted?.id ?? NaN)
  const selectedSummary = latestCompleted ? (latestSummary.data ?? null) : null

  if (dashboard.isPending) return <PageSkeleton />

  if (dashboard.isError) {
    return (
      <div className="space-y-4">
        <PageHeader eyebrow="Overview" title="Dashboard" />
        <Alert
          variant="error"
          title="Workspace unavailable"
          action={
            <Button size="sm" onClick={() => void dashboard.refetch()}>
              Retry
            </Button>
          }
        >
          Unable to load workspace telemetry. Please try again.
        </Alert>
      </div>
    )
  }

  const websiteCount = summary?.websiteCount ?? websites.length
  const auditCount = summary?.auditCount ?? allAudits.length
  const completedCount =
    summary?.completedAuditCount ??
    allAudits.filter((a) => a.status === 'COMPLETED').length
  const failedCount =
    summary?.failedAuditCount ??
    allAudits.filter((a) => a.status === 'FAILED').length

  const activeCount = allAudits.filter((a) => ACTIVE.has(a.status)).length
  const scoredSites = websites.filter(
    (site) => siteStats.get(site.id)?.latestScore !== null,
  ).length
  const unauditedSites = websites.filter(
    (site) => (siteStats.get(site.id)?.auditCount ?? 0) === 0,
  ).length
  const totalPagesCrawled = [...siteStats.values()].reduce(
    (sum, stats) => sum + stats.pagesCrawled,
    0,
  )
  const coveragePct =
    websiteCount === 0 ? 0 : Math.round((scoredSites / websiteCount) * 100)

  const distribution = {
    healthy: websites.filter((site) => {
      const score = siteStats.get(site.id)?.latestScore
      return score !== null && score !== undefined && score >= 80
    }).length,
    needsWork: websites.filter((site) => {
      const score = siteStats.get(site.id)?.latestScore
      return score !== null && score !== undefined && score >= 60 && score < 80
    }).length,
    critical: websites.filter((site) => {
      const score = siteStats.get(site.id)?.latestScore
      return score !== null && score !== undefined && score < 60
    }).length,
    pending: websites.filter(
      (site) => siteStats.get(site.id)?.latestScore == null,
    ).length,
  }

  const rankedSites = websites
    .map((site) => ({
      site,
      stats: siteStats.get(site.id)!,
    }))
    .filter((entry) => entry.stats.latestScore !== null)
    .sort(
      (a, b) => (b.stats.latestScore ?? 0) - (a.stats.latestScore ?? 0),
    )

  const scopedAuditCount = isAll
    ? auditCount
    : (selectedStats?.auditCount ?? 0)
  const scopedCompleted = isAll
    ? completedCount
    : (selectedStats?.completedCount ?? 0)
  const scopedFailed = isAll
    ? failedCount
    : (selectedStats?.failedCount ?? 0)
  const displayScore = isAll ? overviewScore : latestScore

  const phase = resolveWorkflowPhase({
    websiteCount,
    auditCount,
    hasActiveAudit: activeCount > 0,
    hasCompletedAudit: completedCount > 0,
    activeStatus: allAudits.find((a) => ACTIVE.has(a.status))?.status,
  })

  const nextStep =
    phase === 'connect'
      ? {
          title: 'Connect your first website',
          description:
            'Add a domain to unlock crawling, scoring, and issue reports.',
          actionLabel: 'Add website',
          to: '/websites',
          icon: <Globe className="h-4 w-4" />,
        }
      : phase === 'crawl'
        ? {
            title: 'Run your first SEO audit',
            description:
              'Start a crawl to collect pages and generate a health score.',
            actionLabel: 'Start audit',
            to: `/audits${websites[0] ? `?websiteId=${websites[0].id}` : ''}`,
            icon: <Activity className="h-4 w-4" />,
          }
        : phase === 'analyze'
          ? {
              title: 'Audit in progress',
              description:
                'Crawl telemetry is live. Stay here or open the report when analysis finishes.',
              actionLabel: activeCrawl ? 'Open live report' : 'View audits',
              to: activeCrawl ? `/audits/${activeCrawl.id}` : '/audits',
              icon: <Activity className="h-4 w-4" />,
            }
          : {
              title: 'Review ranked SEO issues',
              description:
                'Your latest completed audit is ready. Prioritize errors and warnings next.',
              actionLabel: 'Open issues',
              to: latestCompleted
                ? `/audits/${latestCompleted.id}/issues`
                : '/issues',
              icon: <AlertTriangle className="h-4 w-4" />,
            }

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Overview"
        title="Command center"
        description={`${project.name} — monitor overall SEO health, then drill into any website.`}
        action={
          <>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => void dashboard.refetch()}
              aria-label="Refresh dashboard"
            >
              <RefreshCw className="h-4 w-4" />
            </Button>
            <Link to="/audits">
              <Button variant="secondary" size="sm">
                Audits
              </Button>
            </Link>
            <Link to="/websites">
              <Button size="sm">
                <Plus className="h-4 w-4" />
                Add website
              </Button>
            </Link>
          </>
        }
      />

      <WorkflowRail phase={phase} compact />
      <NextStepBanner {...nextStep} />

      {/* Premium overview panel */}
      <section className="overflow-hidden rounded-2xl border border-default bg-surface shadow-[inset_0_1px_0_0_rgb(255_255_255_/_0.04)]">
        <div className="relative border-b border-default bg-[linear-gradient(125deg,var(--sp-accent-surface),transparent_45%)] px-4 py-5 sm:px-6">
          <div className="flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
            <div className="flex min-w-0 flex-1 flex-col gap-5 sm:flex-row sm:items-center">
              <ScoreRing
                score={displayScore}
                size={132}
                strokeWidth={10}
                label={isAll ? 'avg score' : 'score'}
              />
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="font-mono text-[10px] tracking-[0.14em] text-accent uppercase">
                    Overview
                  </p>
                  <span
                    className={cn(
                      'rounded-full border border-default bg-surface/80 px-2 py-0.5 font-mono text-[10px] uppercase',
                      scoreTone(displayScore),
                    )}
                  >
                    {scoreLabel(displayScore)}
                  </span>
                  {activeCount > 0 && (
                    <span className="inline-flex items-center gap-1.5 rounded-full border border-accent/30 bg-accent-surface px-2 py-0.5 font-mono text-[10px] text-accent uppercase">
                      <span className="relative flex h-1.5 w-1.5">
                        <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-accent opacity-60" />
                        <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-accent" />
                      </span>
                      {activeCount} live
                    </span>
                  )}
                </div>
                <h2 className="mt-2 font-display text-2xl font-semibold tracking-tight text-main">
                  {isAll
                    ? 'Workspace SEO health'
                    : selectedSite?.name || selectedSite?.url || 'Website'}
                </h2>
                <p className="mt-1 max-w-xl text-sm leading-6 text-muted">
                  {isAll
                    ? `Average across ${scoredSites} scored site${scoredSites === 1 ? '' : 's'}. ${unauditedSites > 0 ? `${unauditedSites} still need a first audit.` : 'Every connected site has been scored.'}`
                    : selectedSite
                      ? `Focused view for ${selectedSite.url}. Switch back to overall anytime.`
                      : 'Select a website to inspect its SEO telemetry.'}
                </p>

                <div className="mt-4 max-w-md">
                  <Progress
                    label="Audit coverage"
                    meta={`${scoredSites}/${websiteCount || 0} scored`}
                    value={coveragePct}
                    tone={coveragePct >= 80 ? 'success' : coveragePct >= 40 ? 'accent' : 'warning'}
                  />
                </div>
              </div>
            </div>

            <div className="w-full shrink-0 xl:max-w-sm">
              <Select
                id="dashboard-website"
                label="Focus website"
                value={selectedWebsiteId}
                onChange={(e) => setSelectedWebsiteId(e.target.value)}
                options={[
                  { value: ALL, label: 'All websites (overall)' },
                  ...websites.map((site) => ({
                    value: String(site.id),
                    label: site.name ? `${site.name} — ${site.url}` : site.url,
                  })),
                ]}
              />
              <div className="mt-3 flex flex-wrap gap-2">
                <Button
                  size="sm"
                  variant={isAll ? 'primary' : 'secondary'}
                  onClick={() => setSelectedWebsiteId(ALL)}
                >
                  <Sparkles className="h-3.5 w-3.5" />
                  Overall
                </Button>
                {selectedSite ? (
                  <>
                    <Link to={`/audits?websiteId=${selectedSite.id}`}>
                      <Button size="sm" variant="secondary">
                        <Play className="h-3.5 w-3.5" />
                        Run audit
                      </Button>
                    </Link>
                    <a href={selectedSite.url} target="_blank" rel="noreferrer">
                      <Button size="sm" variant="ghost">
                        <ExternalLink className="h-3.5 w-3.5" />
                        Open
                      </Button>
                    </a>
                  </>
                ) : (
                  <Link to="/websites">
                    <Button size="sm" variant="secondary">
                      <Plus className="h-3.5 w-3.5" />
                      Add site
                    </Button>
                  </Link>
                )}
              </div>
            </div>
          </div>

          {websites.length > 0 && (
            <div className="mt-5 flex gap-2 overflow-x-auto pb-1">
              <FocusChip
                active={isAll}
                label="All websites"
                meta={`${websiteCount}`}
                onClick={() => setSelectedWebsiteId(ALL)}
              />
              {websites.map((site) => {
                const stats = siteStats.get(site.id)
                return (
                  <FocusChip
                    key={site.id}
                    active={selectedWebsiteId === String(site.id)}
                    label={site.name || site.url.replace(/^https?:\/\//, '')}
                    meta={
                      stats?.latestScore != null
                        ? String(stats.latestScore)
                        : '—'
                    }
                    onClick={() => setSelectedWebsiteId(String(site.id))}
                  />
                )
              })}
            </div>
          )}
        </div>

        <div className="grid grid-cols-2 gap-px bg-default/80 md:grid-cols-3 xl:grid-cols-6">
          <OverviewMetric
            label="Websites"
            value={websiteCount}
            hint={isAll ? 'Connected' : 'In workspace'}
            icon={<Globe className="h-4 w-4" />}
          />
          <OverviewMetric
            label="Audits"
            value={isAll ? auditCount : scopedAuditCount}
            hint={isAll ? 'All time' : 'This site'}
            icon={<Activity className="h-4 w-4" />}
          />
          <OverviewMetric
            label="Completed"
            value={scopedCompleted}
            hint="Successful"
            icon={<CheckCircle2 className="h-4 w-4" />}
            tone="text-success"
          />
          <OverviewMetric
            label="Failed"
            value={scopedFailed}
            hint="Need retry"
            icon={<AlertTriangle className="h-4 w-4" />}
            tone="text-critical"
          />
          <OverviewMetric
            label="Live crawls"
            value={
              isAll
                ? activeCount
                : selectedStats?.activeCrawl
                  ? 1
                  : 0
            }
            hint="In progress"
            icon={<RefreshCw className="h-4 w-4" />}
            tone="text-accent"
          />
          <OverviewMetric
            label="Pages crawled"
            value={
              isAll
                ? totalPagesCrawled
                : (selectedStats?.pagesCrawled ?? 0)
            }
            hint="Indexed volume"
            icon={<FileSearch className="h-4 w-4" />}
          />
        </div>

        {isAll && websiteCount > 0 && (
          <div className="grid gap-4 border-t border-default p-4 sm:p-5 lg:grid-cols-12">
            <div className="lg:col-span-5">
              <p className="font-mono text-[10px] tracking-[0.14em] text-dim uppercase">
                Score distribution
              </p>
              <div className="mt-3 space-y-3">
                <DistributionRow
                  label="Healthy"
                  count={distribution.healthy}
                  total={websiteCount}
                  tone="bg-success"
                />
                <DistributionRow
                  label="Needs work"
                  count={distribution.needsWork}
                  total={websiteCount}
                  tone="bg-warning"
                />
                <DistributionRow
                  label="Critical"
                  count={distribution.critical}
                  total={websiteCount}
                  tone="bg-critical"
                />
                <DistributionRow
                  label="Unscored"
                  count={distribution.pending}
                  total={websiteCount}
                  tone="bg-surface-high"
                />
              </div>
            </div>

            <div className="lg:col-span-7">
              <div className="flex items-center justify-between gap-3">
                <p className="font-mono text-[10px] tracking-[0.14em] text-dim uppercase">
                  Website leaderboard
                </p>
                <Link
                  to="/websites"
                  className="text-xs font-medium text-accent hover:text-accent-hover"
                >
                  Manage sites
                </Link>
              </div>
              {rankedSites.length === 0 ? (
                <p className="mt-4 text-sm text-muted">
                  No scored websites yet. Run an audit to populate the
                  leaderboard.
                </p>
              ) : (
                <ul className="mt-3 divide-y divide-default/70 overflow-hidden rounded-xl border border-default">
                  {rankedSites.slice(0, 5).map((entry, index) => (
                    <li key={entry.site.id}>
                      <button
                        type="button"
                        onClick={() =>
                          setSelectedWebsiteId(String(entry.site.id))
                        }
                        className="flex w-full items-center gap-3 px-3 py-2.5 text-left transition-colors hover:bg-surface-low"
                      >
                        <span className="w-5 font-mono text-xs text-dim font-tabular">
                          {index + 1}
                        </span>
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-medium text-main">
                            {entry.site.name || entry.site.url}
                          </p>
                          <p className="truncate font-mono text-[10px] text-dim">
                            {entry.site.url}
                          </p>
                        </div>
                        <span
                          className={cn(
                            'font-mono text-sm font-semibold font-tabular',
                            scoreTone(entry.stats.latestScore),
                          )}
                        >
                          {entry.stats.latestScore}
                        </span>
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        )}

        {!isAll && selectedSummary && (
          <div className="grid grid-cols-2 gap-3 border-t border-default p-4 sm:grid-cols-4 sm:p-5">
            <MiniStat label="Total issues" value={selectedSummary.totalIssues} />
            <MiniStat
              label="Errors"
              value={selectedSummary.errorCount}
              tone="text-critical"
            />
            <MiniStat
              label="Warnings"
              value={selectedSummary.warningCount}
              tone="text-warning"
            />
            <MiniStat
              label="Info"
              value={selectedSummary.infoCount}
              tone="text-info"
            />
          </div>
        )}
      </section>

      <div className="grid gap-4 lg:grid-cols-12">
        <Card
          className="lg:col-span-5"
          title="Latest report"
          description={
            isAll
              ? 'Most recent completed audit in the workspace'
              : 'Most recent completed audit for this website'
          }
        >
          <div className="flex flex-col items-center gap-4 p-5 sm:flex-row sm:items-center">
            <ScoreRing
              score={latestCompleted?.score ?? null}
              size={112}
              strokeWidth={8}
            />
            <div className="min-w-0 flex-1 text-center sm:text-left">
              {latestCompleted ? (
                <>
                  <p className="truncate font-mono text-xs text-muted">
                    {latestCompleted.websiteUrl}
                  </p>
                  <p className="mt-2 text-sm text-muted">
                    Audit #{latestCompleted.id}
                    {latestCompleted.pagesCrawled > 0 &&
                      ` · ${latestCompleted.pagesCrawled} pages crawled`}
                  </p>
                  <div className="mt-3 flex flex-wrap justify-center gap-3 sm:justify-start">
                    <Link
                      to={`/audits/${latestCompleted.id}`}
                      className="inline-flex items-center gap-1 text-sm font-medium text-accent hover:text-accent-hover"
                    >
                      Open report
                      <ArrowRight className="h-3.5 w-3.5" />
                    </Link>
                    <Link
                      to={`/audits/${latestCompleted.id}/issues`}
                      className="inline-flex items-center gap-1 text-sm font-medium text-muted hover:text-main"
                    >
                      <FileWarning className="h-3.5 w-3.5" />
                      Issues
                    </Link>
                    <Link
                      to={`/audits/${latestCompleted.id}/pages`}
                      className="inline-flex items-center gap-1 text-sm font-medium text-muted hover:text-main"
                    >
                      <FileSearch className="h-3.5 w-3.5" />
                      Pages
                    </Link>
                  </div>
                </>
              ) : (
                <EmptyState
                  className="min-h-0 py-2"
                  title="No completed audits"
                  description={
                    isAll
                      ? 'Run an audit on any website to generate a health score.'
                      : 'Run an audit for this website to generate a health score.'
                  }
                  action={
                    <Link
                      to={
                        selectedSite
                          ? `/audits?websiteId=${selectedSite.id}`
                          : websites.length
                            ? '/audits'
                            : '/websites'
                      }
                    >
                      <Button size="sm">
                        {websites.length ? 'Start audit' : 'Add website'}
                      </Button>
                    </Link>
                  }
                />
              )}
            </div>
          </div>
        </Card>

        <div className="lg:col-span-7">
          {activeCrawl ? (
            <CrawlProgress
              status={activeCrawl.status}
              pagesCrawled={activeCrawl.pagesCrawled}
              pagesAnalyzed={activeCrawl.pagesAnalyzed}
              websiteUrl={activeCrawl.websiteUrl}
            />
          ) : (
            <Card
              title="Crawl telemetry"
              description={
                isAll
                  ? 'No active crawl in this workspace'
                  : 'No active crawl for this website'
              }
            >
              <div className="p-5">
                <p className="text-sm text-muted">
                  Start an audit to stream live crawl progress, page throughput,
                  and analysis status.
                </p>
                <Link
                  to={
                    selectedSite
                      ? `/audits?websiteId=${selectedSite.id}`
                      : '/audits'
                  }
                  className="mt-3 inline-block"
                >
                  <Button size="sm" variant="secondary">
                    Go to audits
                  </Button>
                </Link>
              </div>
            </Card>
          )}
        </div>
      </div>

      <div className="grid gap-4 xl:grid-cols-2">
        <Card
          title="Websites"
          description="Select a property to focus the overview"
          action={
            <Link to="/websites" className="text-xs font-medium text-accent">
              Manage
            </Link>
          }
        >
          {websites.length === 0 ? (
            <EmptyState
              icon={<Globe className="h-5 w-5" />}
              title="No websites yet"
              description="Add a website to begin crawling and auditing."
              action={
                <Link to="/websites">
                  <Button size="sm">
                    <Plus className="h-4 w-4" />
                    Add website
                  </Button>
                </Link>
              }
            />
          ) : (
            <ul className="divide-y divide-default/60">
              {websites.map((site) => {
                const stats = siteStats.get(site.id)
                const selected = selectedWebsiteId === String(site.id)
                return (
                  <li key={site.id}>
                    <button
                      type="button"
                      onClick={() => setSelectedWebsiteId(String(site.id))}
                      className={cn(
                        'flex w-full items-center justify-between gap-3 px-4 py-3 text-left transition-colors sm:px-5',
                        selected ? 'bg-accent-surface' : 'hover:bg-surface-low',
                      )}
                    >
                      <div className="min-w-0">
                        <p className="truncate text-sm font-medium text-main">
                          {site.name || site.url}
                        </p>
                        <p className="mt-0.5 truncate font-mono text-[10px] text-dim">
                          {site.url}
                          {stats ? ` · ${stats.auditCount} audits` : ''}
                          {stats?.latestScore != null
                            ? ` · score ${stats.latestScore}`
                            : ''}
                        </p>
                      </div>
                      <div className="flex shrink-0 items-center gap-2">
                        {stats?.activeCrawl && (
                          <StatusBadge status={stats.activeCrawl.status} />
                        )}
                        <StatusBadge status={site.status ?? 'ACTIVE'} />
                      </div>
                    </button>
                  </li>
                )
              })}
            </ul>
          )}
        </Card>

        <Card
          title={isAll ? 'Recent audits' : 'Website audits'}
          description={
            isAll
              ? 'Latest crawl activity across all sites'
              : `History for ${selectedSite?.name || selectedSite?.url || 'website'}`
          }
          action={
            <Link
              to={
                selectedSite
                  ? `/audits?websiteId=${selectedSite.id}`
                  : '/audits'
              }
              className="text-xs font-medium text-accent"
            >
              View all
            </Link>
          }
        >
          {scopedAudits.length === 0 ? (
            <EmptyState
              icon={<Activity className="h-5 w-5" />}
              title="No audits yet"
              description="Run an audit to populate activity for this view."
              action={
                <Link
                  to={
                    selectedSite
                      ? `/audits?websiteId=${selectedSite.id}`
                      : '/audits'
                  }
                >
                  <Button size="sm">Start audit</Button>
                </Link>
              }
            />
          ) : (
            <ul className="divide-y divide-default/60">
              {scopedAudits.slice(0, 8).map((audit) => (
                <li key={audit.id}>
                  <Link
                    to={`/audits/${audit.id}`}
                    className="flex items-center justify-between gap-3 px-4 py-3 transition-colors hover:bg-surface-low sm:px-5"
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-main">
                        {audit.websiteUrl}
                      </p>
                      <p className="mt-0.5 font-mono text-[10px] text-dim">
                        Audit #{audit.id}
                        {audit.score !== null && ` · Score ${audit.score}`}
                        {audit.pagesCrawled > 0 &&
                          ` · ${audit.pagesCrawled} pages`}
                      </p>
                    </div>
                    <StatusBadge status={audit.status} />
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </div>
  )
}

function OverviewMetric({
  label,
  value,
  hint,
  icon,
  tone = 'text-main',
}: {
  label: string
  value: number
  hint: string
  icon: ReactNode
  tone?: string
}) {
  return (
    <div className="bg-surface px-4 py-4">
      <div className="flex items-center justify-between gap-2">
        <span className="text-muted">{icon}</span>
        <span className="font-mono text-[10px] tracking-wider text-dim uppercase">
          {label}
        </span>
      </div>
      <p className={cn('mt-3 font-display text-2xl font-semibold font-tabular', tone)}>
        {value.toLocaleString()}
      </p>
      <p className="mt-1 text-xs text-muted">{hint}</p>
    </div>
  )
}

function DistributionRow({
  label,
  count,
  total,
  tone,
}: {
  label: string
  count: number
  total: number
  tone: string
}) {
  const pct = total === 0 ? 0 : Math.round((count / total) * 100)
  return (
    <div>
      <div className="mb-1.5 flex items-center justify-between gap-2">
        <span className="text-sm text-main">{label}</span>
        <span className="font-mono text-[11px] text-dim font-tabular">
          {count} · {pct}%
        </span>
      </div>
      <div className="h-1.5 overflow-hidden rounded-full bg-surface-elevated">
        <div
          className={cn('h-full rounded-full transition-all duration-500', tone)}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  )
}

function FocusChip({
  label,
  meta,
  active,
  onClick,
}: {
  label: string
  meta?: string
  active: boolean
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'inline-flex shrink-0 items-center gap-2 rounded-full border px-3 py-1.5 text-left transition-colors',
        active
          ? 'border-accent bg-surface text-accent shadow-[0_0_0_3px_var(--sp-accent-surface)]'
          : 'border-default/80 bg-surface/70 text-muted hover:border-accent/40 hover:text-main',
      )}
    >
      <span className="max-w-[11rem] truncate text-xs font-medium">{label}</span>
      {meta && (
        <span className="rounded bg-surface-elevated px-1.5 py-0.5 font-mono text-[10px] text-dim font-tabular">
          {meta}
        </span>
      )}
    </button>
  )
}

function MiniStat({
  label,
  value,
  tone = 'text-main',
}: {
  label: string
  value: number
  tone?: string
}) {
  return (
    <div className="rounded-lg border border-default bg-surface-low px-2.5 py-2">
      <p className="font-mono text-[9px] tracking-wider text-dim uppercase">
        {label}
      </p>
      <p className={cn('mt-1 font-mono text-sm font-semibold font-tabular', tone)}>
        {value.toLocaleString()}
      </p>
    </div>
  )
}
