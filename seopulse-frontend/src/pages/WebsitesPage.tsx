import { useState } from 'react'
import {
  Activity,
  ChevronRight,
  ExternalLink,
  Gauge,
  Globe,
  Plus,
  RefreshCw,
} from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'

import { isActiveAudit, type Audit } from '@/api/audits'
import { useDashboard } from '@/api/queries/dashboard'
import { websiteApi, type Website } from '@/api/websites'
import { AddWebsiteModal } from '@/features/websites/components/AddWebsiteModal'
import { hostOf, relativeTime } from '@/lib/format'
import { useToast } from '@/lib/toast'
import { useWorkspace } from '@/lib/workspace'

import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { IconTile } from '@/components/ui/IconTile'
import { siteTint } from '@/components/ui/tints'
import { NextStepBanner } from '@/components/ui/NextStepBanner'
import { PageHeader } from '@/components/ui/PageHeader'
import { Progress } from '@/components/ui/Progress'
import { ScoreRing } from '@/components/ui/ScoreRing'
import { CardSkeleton } from '@/components/ui/Skeleton'
import { StatTile } from '@/components/ui/StatTile'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { WorkflowRail } from '@/components/ui/WorkflowRail'

const NO_AUDITS: Audit[] = []

export function WebsitesPage() {
  const { projectId, notifyDataChanged } = useWorkspace()
  const { pushToast } = useToast()
  const navigate = useNavigate()
  const dashboard = useDashboard(projectId)
  const [showAddModal, setShowAddModal] = useState(false)

  const websites = dashboard.data?.websites ?? []
  const auditsBySite = dashboard.data?.auditsBySite ?? {}
  const loading = dashboard.isPending

  async function handleCreateWebsite(data: { name: string; url: string }) {
    const website = await websiteApi.createWebsite(projectId, data)
    notifyDataChanged()
    pushToast({
      tone: 'success',
      title: 'Website added',
      description: `${website.url} is ready to audit.`,
    })
    navigate(`/audits?websiteId=${website.id}`)
  }

  const scores = websites
    .map((site) => latestCompleted(auditsBySite[site.id])?.score)
    .filter((score): score is number => typeof score === 'number')
  const averageScore = scores.length
    ? Math.round(scores.reduce((sum, score) => sum + score, 0) / scores.length)
    : null
  const totalAudits = Object.values(auditsBySite).reduce((sum, audits) => sum + audits.length, 0)
  const running = Object.values(auditsBySite).flat().filter((audit) => isActiveAudit(audit.status)).length

  return (
    <>
      <div className="space-y-8">
        <PageHeader
          eyebrow="Workspace"
          title="Websites"
          description="Every property you monitor, with its latest health at a glance."
          action={
            <Button onClick={() => setShowAddModal(true)}>
              <Plus className="h-4 w-4" />
              Add website
            </Button>
          }
        />

        {!loading && websites.length === 0 && (
          <>
            <WorkflowRail phase="connect" />
            <NextStepBanner
              title="Add a website to begin"
              description="Paste a public URL. We’ll attach it to this workspace and unlock audits."
              actionLabel="Add website"
              icon={<Globe className="h-4 w-4" />}
              onAction={() => setShowAddModal(true)}
            />
          </>
        )}

        {dashboard.isError && (
          <Alert
            variant="error"
            title="Failed to load websites"
            action={
              <Button size="sm" variant="secondary" onClick={() => void dashboard.refetch()}>
                <RefreshCw className="h-3.5 w-3.5" />
                Retry
              </Button>
            }
          >
            Unable to load websites. Please try again.
          </Alert>
        )}

        {loading ? (
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            <CardSkeleton />
            <CardSkeleton />
            <CardSkeleton />
          </div>
        ) : websites.length === 0 ? (
          <div className="widget">
            <EmptyState
              icon={<Globe className="h-6 w-6" />}
              title="No websites yet"
              description="Add your first website to start running SEO audits."
              action={
                <Button onClick={() => setShowAddModal(true)}>
                  <Plus className="h-4 w-4" />
                  Add website
                </Button>
              }
            />
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <StatTile
                tint="blue"
                icon={<Globe />}
                label="Connected"
                value={websites.length}
                caption={`${websites.length === 1 ? 'Website' : 'Websites'} in this workspace`}
              />
              <StatTile
                tint="pink"
                icon={<Gauge />}
                label="Average health"
                value={averageScore ?? '—'}
                caption={scores.length ? `Across ${scores.length} scored site${scores.length === 1 ? '' : 's'}` : 'Run an audit to score'}
              />
              <StatTile
                tint="green"
                icon={<Activity />}
                label="Audits"
                value={totalAudits}
                caption={running ? `${running} running now` : 'Recent history'}
                to="/audits"
              />
            </div>

            <section>
              <div className="mb-3 px-1">
                <h2 className="text-headline text-main">Your websites</h2>
                <p className="mt-0.5 text-xs text-dim">Latest completed audit for each property</p>
              </div>
              <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                {websites.map((site) => (
                  <SiteCard key={site.id} site={site} audits={auditsBySite[site.id] ?? NO_AUDITS} />
                ))}
                <li>
                  <button
                    type="button"
                    onClick={() => setShowAddModal(true)}
                    className="group flex h-full min-h-[232px] w-full flex-col items-center justify-center gap-3 rounded-3xl border-2 border-dashed border-default text-muted transition-colors hover:border-accent/50 hover:bg-accent-surface hover:text-accent"
                  >
                    <span className="flex h-12 w-12 items-center justify-center rounded-full bg-surface-elevated transition-colors group-hover:bg-accent group-hover:text-white">
                      <Plus className="h-5 w-5" />
                    </span>
                    <span className="text-sm font-semibold">Connect another website</span>
                  </button>
                </li>
              </ul>
            </section>
          </>
        )}
      </div>

      <AddWebsiteModal
        open={showAddModal}
        onClose={() => setShowAddModal(false)}
        onSubmit={handleCreateWebsite}
      />
    </>
  )
}

function latestCompleted(audits: Audit[] | undefined) {
  return audits?.find((audit) => audit.status === 'COMPLETED') ?? null
}

function SiteCard({ site, audits }: { site: Website; audits: Audit[] }) {
  const latest = latestCompleted(audits)
  const active = audits.find((audit) => isActiveAudit(audit.status)) ?? null
  const host = hostOf(site.url)
  const progress =
    active && active.pagesCrawled > 0
      ? Math.round((active.pagesAnalyzed / active.pagesCrawled) * 100)
      : 8

  return (
    <li className="widget group flex flex-col p-5 transition-transform duration-300 hover:-translate-y-0.5">
      <div className="flex items-start gap-3">
        <IconTile tint={siteTint(site.id)} size="lg">
          {host.charAt(0)}
        </IconTile>
        <div className="min-w-0 flex-1">
          <p className="truncate text-[15px] font-semibold text-main">{site.name || host}</p>
          <p className="truncate text-xs text-dim">{site.url}</p>
        </div>
        {site.status && <StatusBadge status={site.status} />}
      </div>

      <div className="mt-5 flex flex-1 items-center gap-4">
        {active ? (
          <div className="w-full rounded-2xl bg-surface-low p-3.5 dark:bg-surface-elevated/50">
            <div className="mb-2 flex items-center justify-between">
              <StatusBadge status={active.status} />
              <span className="text-xs text-dim">{active.pagesCrawled} pages</span>
            </div>
            <Progress value={progress} tone="accent" />
          </div>
        ) : (
          <>
            <ScoreRing score={latest?.score ?? null} size={72} strokeWidth={7} />
            <div className="min-w-0">
              <p className="text-[13px] font-semibold text-main">
                {latest ? 'Health score' : 'Not audited yet'}
              </p>
              <p className="mt-0.5 text-xs text-dim">
                {latest
                  ? `${latest.pagesCrawled} page${latest.pagesCrawled === 1 ? '' : 's'} · ${relativeTime(latest.completedAt ?? latest.createdAt)}`
                  : 'Run an audit to score this site'}
              </p>
              <p className="mt-0.5 text-xs text-dim">
                {audits.length} audit{audits.length === 1 ? '' : 's'} run
              </p>
            </div>
          </>
        )}
      </div>

      <div className="mt-5 flex items-center gap-2 border-t border-default pt-4">
        <Link
          to={`/audits?websiteId=${site.id}`}
          className="inline-flex h-8 items-center gap-1 rounded-full bg-surface-elevated px-3.5 text-[13px] font-semibold text-main transition-colors hover:bg-surface-high"
        >
          Audits
          <ChevronRight className="h-3.5 w-3.5" />
        </Link>
        {latest && (
          <Link
            to={`/audits/${latest.id}`}
            className="inline-flex h-8 items-center rounded-full px-3 text-[13px] font-semibold text-accent transition-opacity hover:opacity-75"
          >
            Latest report
          </Link>
        )}
        <a
          href={site.url}
          target="_blank"
          rel="noreferrer"
          aria-label={`Open ${host} in a new tab`}
          className="ml-auto flex h-8 w-8 items-center justify-center rounded-full text-dim transition-colors hover:bg-surface-elevated hover:text-main"
        >
          <ExternalLink className="h-4 w-4" />
        </a>
      </div>
    </li>
  )
}
