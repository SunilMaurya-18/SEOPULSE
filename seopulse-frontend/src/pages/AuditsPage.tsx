import { useState } from 'react'
import {
  ExternalLink,
  FileSearch,
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
import { useToast } from '@/lib/toast'
import { useWorkspace } from '@/lib/workspace'

import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { DownloadReportButton } from '@/components/ui/DownloadReportButton'
import { EmptyState } from '@/components/ui/EmptyState'
import { NextStepBanner } from '@/components/ui/NextStepBanner'
import { PageHeader } from '@/components/ui/PageHeader'
import { Progress } from '@/components/ui/Progress'
import { Select } from '@/components/ui/Select'
import { TableSkeleton } from '@/components/ui/Skeleton'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { WorkflowRail } from '@/components/ui/WorkflowRail'
import {
  Table,
  TBody,
  TD,
  TH,
  THead,
  TR,
  TableToolbar,
} from '@/components/ui/Table'
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
  const hasCompleted = audits.some((audit) => audit.status === 'COMPLETED')
  const phase =
    websites.length === 0
      ? 'connect'
      : hasActive
        ? audits.find((a) => a.status === 'ANALYZING')
          ? 'analyze'
          : 'crawl'
        : hasCompleted
          ? 'report'
          : 'crawl'

  return (
    <div className="space-y-6">
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

      <WorkflowRail phase={phase} />

      {websites.length === 0 ? (
        <NextStepBanner
          title="Connect a website first"
          description="Audits need a property. Add one, then come back to start crawling."
          actionLabel="Add website"
          to="/websites"
          icon={<Globe className="h-4 w-4" />}
        />
      ) : audits.length === 0 ? (
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
            <Button
              size="sm"
              variant="secondary"
              onClick={retry}
            >
              <RefreshCw className="h-3.5 w-3.5" />
              Retry
            </Button>
          }
        >
          {error}
        </Alert>
      )}

      <Card
        title="Website"
        description="Choose the website whose audits you want to inspect."
        padded
      >
        {loadingWebsites ? (
          <div className="h-9 animate-pulse rounded bg-surface-elevated" />
        ) : websites.length === 0 ? (
          <EmptyState
            icon={<Globe className="h-5 w-5" />}
            title="No websites available"
            description="Add a website before running your first SEO audit."
            action={
              <Link to="/websites">
                <Button size="sm">Go to websites</Button>
              </Link>
            }
          />
        ) : (
          <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <div className="w-full sm:max-w-md">
              <Select
                id="audit-website"
                label="Website"
                value={selectedWebsiteId?.toString() ?? ''}
                onChange={(e) =>
                  setSelectedWebsiteId(Number(e.target.value))
                }
                options={websites.map((w) => ({
                  value: String(w.id),
                  label: w.name ? `${w.name} — ${w.url}` : w.url,
                }))}
              />
            </div>
            {selectedWebsite && (
              <a
                href={selectedWebsite.url}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-1.5 text-sm font-medium text-muted hover:text-accent"
              >
                Open website
                <ExternalLink className="h-3.5 w-3.5" />
              </a>
            )}
          </div>
        )}
      </Card>

      <Card
        title="Audit history"
        description={
          selectedWebsite
            ? `Previous audits for ${selectedWebsite.url}`
            : 'Previous SEO audits.'
        }
        className="overflow-hidden"
      >
        {loadingAudits ? (
          <TableSkeleton rows={5} />
        ) : websites.length === 0 ? (
          <EmptyState
            icon={<Globe className="h-5 w-5" />}
            title="No websites to audit"
            description="Connect a website first, then return here to run audits."
          />
        ) : audits.length === 0 ? (
          <EmptyState
            icon={<FileSearch className="h-5 w-5" />}
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
          <>
            <TableToolbar>
              <p className="font-mono text-[11px] text-muted">
                {audits.length} audit{audits.length === 1 ? '' : 's'}
              </p>
            </TableToolbar>
            <Table>
              <THead>
                <TR className="hover:bg-transparent">
                  <TH>Audit</TH>
                  <TH>Status</TH>
                  <TH>Progress</TH>
                  <TH>Score</TH>
                  <TH>Created</TH>
                  <TH className="text-right">Actions</TH>
                </TR>
              </THead>
              <TBody>
                {audits.map((audit) => {
                  const inProgress = isActiveAudit(audit.status)
                  const progress = getProgress(audit)

                  return (
                    <TR key={audit.id} className="group">
                      <TD>
                        <Link
                          to={`/audits/${audit.id}`}
                          className="block min-w-0"
                        >
                          <p className="text-sm font-medium text-main group-hover:text-accent">
                            Audit #{audit.id}
                          </p>
                          <p className="mt-0.5 truncate font-mono text-[11px] text-dim">
                            {audit.websiteUrl}
                          </p>
                        </Link>
                      </TD>
                      <TD>
                        <Link to={`/audits/${audit.id}`}>
                          <StatusBadge status={audit.status} />
                        </Link>
                      </TD>
                      <TD>
                        <Link
                          to={`/audits/${audit.id}`}
                          className="block min-w-40"
                        >
                          {inProgress ? (
                            <Progress
                              value={progress}
                              label="Crawl"
                              meta={`${audit.pagesAnalyzed}/${audit.pagesCrawled}`}
                              tone="accent"
                            />
                          ) : (
                            <span className="font-mono text-xs text-muted font-tabular">
                              {audit.pagesAnalyzed} / {audit.pagesCrawled}
                            </span>
                          )}
                        </Link>
                      </TD>
                      <TD>
                        <Link to={`/audits/${audit.id}`}>
                          <span
                            className={cn(
                              'font-display text-sm font-semibold font-tabular',
                              audit.score === null
                                ? 'text-dim'
                                : 'text-main',
                            )}
                          >
                            {audit.score !== null ? audit.score : '—'}
                          </span>
                        </Link>
                      </TD>
                      <TD mono>
                        <Link to={`/audits/${audit.id}`}>
                          {formatDate(audit.createdAt)}
                        </Link>
                      </TD>
                      <TD className="text-right">
                        <div className="inline-flex items-center justify-end gap-2">
                          <Link to={`/audits/${audit.id}`}>
                            <Button size="sm" variant="ghost">
                              Open
                            </Button>
                          </Link>
                          {!inProgress && (
                            <DownloadReportButton
                              projectId={projectId}
                              auditId={audit.id}
                            />
                          )}
                        </div>
                      </TD>
                    </TR>
                  )
                })}
              </TBody>
            </Table>
          </>
        )}
      </Card>
    </div>
  )
}

function getProgress(audit: Audit) {
  if (audit.pagesCrawled === 0) return 0
  return Math.min(
    100,
    Math.round((audit.pagesAnalyzed / audit.pagesCrawled) * 100),
  )
}

function formatDate(date: string) {
  return new Date(date).toLocaleString()
}
