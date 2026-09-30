import { Check } from 'lucide-react'

import { cn } from '@/lib/cn'
import { Progress } from './Progress'
import { StatusBadge } from './StatusBadge'

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
const STAGE_LABELS: Record<(typeof STAGES)[number], string> = {
  QUEUED: 'Queued',
  CRAWLING: 'Crawling',
  ANALYZING: 'Analyzing',
  COMPLETED: 'Ready',
}

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
      ? 'Waiting for a crawler worker'
      : normalized === 'CRAWLING'
        ? 'Crawling pages and extracting metadata'
        : normalized === 'ANALYZING'
          ? 'Analyzing SEO signals and scoring issues'
          : normalized === 'COMPLETED'
            ? 'Audit complete — report is ready'
            : normalized === 'FAILED'
              ? 'Audit failed — review the error and retry'
              : status.replace(/_/g, ' ')

  const radius = 34
  const circumference = 2 * Math.PI * radius

  return (
    <section
      className={cn('widget relative overflow-hidden p-5 sm:p-6', className)}
      aria-live="polite"
    >
      {running && (
        <div className="pointer-events-none absolute -top-24 -left-16 h-56 w-56 rounded-full bg-accent/20 blur-3xl" />
      )}

      <div className="relative flex flex-col gap-6 md:flex-row md:items-center">
        <div className="relative h-20 w-20 shrink-0">
          <svg viewBox="0 0 80 80" className="h-20 w-20 -rotate-90">
            <circle cx="40" cy="40" r={radius} fill="none" strokeWidth="7" className="stroke-surface-elevated" />
            <circle
              cx="40"
              cy="40"
              r={radius}
              fill="none"
              strokeWidth="7"
              strokeLinecap="round"
              strokeDasharray={circumference}
              strokeDashoffset={circumference - (pct / 100) * circumference}
              className={cn(
                'transition-all duration-700 ease-out',
                failed ? 'stroke-critical' : running ? 'stroke-accent' : 'stroke-success',
              )}
            />
          </svg>
          <span className="num absolute inset-0 flex items-center justify-center text-lg font-bold text-main">
            {pct.toFixed(0)}%
          </span>
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            {running && (
              <span className="relative flex h-2 w-2">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-accent opacity-60" />
                <span className="relative inline-flex h-2 w-2 rounded-full bg-accent" />
              </span>
            )}
            <p className="text-headline text-main">
              {running ? 'Live crawl' : failed ? 'Crawl failed' : 'Crawl complete'}
            </p>
            <StatusBadge status={status} />
          </div>
          <p className="mt-1 text-sm text-muted">{stageCopy}</p>
          {websiteUrl && <p className="mt-0.5 truncate text-xs text-dim">{websiteUrl}</p>}

          <ol className="mt-4 flex items-center">
            {STAGES.map((stage, index) => {
              const done = !failed && index < stageIdx
              const active = !failed && index === stageIdx
              return (
                <li key={stage} className={cn('flex items-center', index < STAGES.length - 1 && 'flex-1')}>
                  <span className="flex items-center gap-1.5">
                    <span
                      className={cn(
                        'flex h-5 w-5 items-center justify-center rounded-full text-[10px] font-bold',
                        done && 'bg-success text-white',
                        active && (running ? 'bg-accent text-white' : 'bg-success text-white'),
                        !done && !active && 'bg-surface-elevated text-dim',
                      )}
                    >
                      {done || (active && !running) ? <Check className="h-3 w-3" strokeWidth={3} /> : index + 1}
                    </span>
                    <span
                      className={cn(
                        'hidden text-xs font-medium sm:inline',
                        active ? 'text-main' : done ? 'text-muted' : 'text-dim',
                      )}
                    >
                      {STAGE_LABELS[stage]}
                    </span>
                  </span>
                  {index < STAGES.length - 1 && (
                    <span
                      className={cn(
                        'mx-2 h-0.5 flex-1 rounded-full',
                        done ? 'bg-success' : 'bg-surface-elevated',
                        active && running && 'progress-shimmer',
                      )}
                    />
                  )}
                </li>
              )
            })}
          </ol>

          <Progress
            className="mt-4"
            value={pct}
            tone={failed ? 'critical' : running ? 'accent' : 'success'}
          />
        </div>

        <dl className="grid grid-cols-4 gap-2 md:w-[300px] md:grid-cols-2">
          <TelemetryCell
            label="Pages"
            value={
              targetPages
                ? `${pagesCrawled.toLocaleString()} / ${targetPages.toLocaleString()}`
                : pagesCrawled.toLocaleString()
            }
          />
          <TelemetryCell label="Analyzed" value={pagesAnalyzed.toLocaleString()} />
          <TelemetryCell label="Errors" value={String(errorCount)} tone="text-critical" />
          <TelemetryCell label="Warnings" value={String(warningCount)} tone="text-warning" />
        </dl>
      </div>
    </section>
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
    <div className="rounded-2xl bg-surface-low px-3 py-2.5 dark:bg-surface-elevated/50">
      <dt className="text-[11px] font-medium text-dim">{label}</dt>
      <dd className={cn('num mt-0.5 text-base font-bold', tone)}>{value}</dd>
    </div>
  )
}
