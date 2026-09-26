import { cn } from '@/lib/cn'
import { Progress } from './Progress'
import { Badge } from './Badge'

interface CrawlProgressProps {
  status: string
  pagesCrawled: number
  pagesAnalyzed?: number
  targetPages?: number
  errorCount?: number
  warningCount?: number
  className?: string
  websiteUrl?: string
}

const STAGES = ['QUEUED', 'CRAWLING', 'ANALYZING', 'COMPLETED'] as const

export function CrawlProgress({
  status,
  pagesCrawled,
  pagesAnalyzed = 0,
  targetPages,
  errorCount = 0,
  warningCount = 0,
  className,
  websiteUrl,
}: CrawlProgressProps) {
  const normalized = status.toUpperCase()
  const running =
    normalized === 'QUEUED' ||
    normalized === 'CRAWLING' ||
    normalized === 'ANALYZING'
  const failed = normalized === 'FAILED'
  const stageIdx = failed
    ? -1
    : Math.max(
        0,
        STAGES.indexOf(
          (STAGES.includes(normalized as (typeof STAGES)[number])
            ? normalized
            : 'QUEUED') as (typeof STAGES)[number],
        ),
      )

  const pct =
    targetPages && targetPages > 0
      ? Math.min(100, (pagesCrawled / targetPages) * 100)
      : normalized === 'COMPLETED'
        ? 100
        : normalized === 'ANALYZING'
          ? Math.min(92, 55 + Math.min(pagesAnalyzed / 40, 30))
          : Math.min(80, pagesCrawled > 0 ? 28 + Math.min(pagesCrawled / 40, 45) : 10)

  const stageCopy =
    normalized === 'QUEUED'
      ? 'Queued — waiting for a crawler worker'
      : normalized === 'CRAWLING'
        ? 'Crawling pages and extracting metadata'
        : normalized === 'ANALYZING'
          ? 'Analyzing SEO signals and scoring issues'
          : normalized === 'COMPLETED'
            ? 'Audit complete — report is ready'
            : normalized === 'FAILED'
              ? 'Audit failed — review the error and retry'
              : status.replace(/_/g, ' ')

  return (
    <div
      className={cn(
        'rounded-xl border border-default bg-surface p-4 sm:p-5',
        className,
      )}
    >
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            {running && (
              <span className="relative flex h-2 w-2">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-accent opacity-60" />
                <span className="relative inline-flex h-2 w-2 rounded-full bg-accent" />
              </span>
            )}
            <p className="font-mono text-[11px] font-medium tracking-wider text-main uppercase">
              {running ? 'Live crawl' : failed ? 'Crawl failed' : 'Crawl complete'}
            </p>
          </div>
          <p className="mt-1 text-sm text-muted">{stageCopy}</p>
          {websiteUrl && (
            <p className="mt-1 truncate font-mono text-[11px] text-dim">
              {websiteUrl}
            </p>
          )}
        </div>
        <Badge
          variant={
            failed
              ? 'critical'
              : running
                ? 'accent'
                : 'success'
          }
        >
          {status}
        </Badge>
      </div>

      <ol className="mt-5 grid grid-cols-4 gap-2">
        {STAGES.map((stage, index) => {
          const done = !failed && index < stageIdx
          const active = !failed && index === stageIdx
          return (
            <li key={stage} className="min-w-0">
              <div
                className={cn(
                  'h-1 rounded-full transition-colors',
                  done || active ? 'bg-accent' : 'bg-surface-elevated',
                  active && running && 'progress-shimmer',
                  failed && 'bg-critical/40',
                )}
              />
              <p
                className={cn(
                  'mt-2 truncate font-mono text-[10px] tracking-wide uppercase',
                  active ? 'text-accent' : done ? 'text-main' : 'text-dim',
                )}
              >
                {stage.toLowerCase()}
              </p>
            </li>
          )
        })}
      </ol>

      <Progress
        className="mt-4"
        value={pct}
        meta={`${pct.toFixed(0)}%`}
        tone={failed ? 'critical' : running ? 'accent' : 'success'}
      />

      <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <TelemetryCell
          label="Pages"
          value={
            targetPages
              ? `${pagesCrawled.toLocaleString()} / ${targetPages.toLocaleString()}`
              : pagesCrawled.toLocaleString()
          }
        />
        <TelemetryCell label="Analyzed" value={pagesAnalyzed.toLocaleString()} />
        <TelemetryCell
          label="Errors"
          value={String(errorCount)}
          tone="text-critical"
        />
        <TelemetryCell
          label="Warnings"
          value={String(warningCount)}
          tone="text-warning"
        />
      </div>
    </div>
  )
}

function TelemetryCell({
  label,
  value,
  tone = 'text-main',
}: {
  label: string
  value: string
  tone?: string
}) {
  return (
    <div className="rounded-lg border border-default bg-surface-low px-3 py-2.5">
      <p className="font-mono text-[10px] tracking-wider text-dim uppercase">
        {label}
      </p>
      <p className={cn('mt-1 font-mono text-sm font-semibold font-tabular', tone)}>
        {value}
      </p>
    </div>
  )
}
