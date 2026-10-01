import { useState, type ReactNode } from 'react'
import { ArrowDownRight, ArrowUpRight, CheckCircle2, GitCompareArrows, PlusCircle } from 'lucide-react'
import { Link } from 'react-router-dom'

import type { AuditComparison, IssueChange } from '@/api/audits'
import { Badge } from '@/components/ui/Badge'
import { cn } from '@/lib/cn'
import { formatDateTime, pathOf } from '@/lib/format'

const PREVIEW = 5

function severityVariant(severity: string) {
  const s = severity.toUpperCase()
  if (s === 'ERROR' || s === 'CRITICAL') return 'critical' as const
  if (s === 'WARNING' || s === 'WARN') return 'warning' as const
  return 'info' as const
}

export function ComparisonPanel({ comparison }: { comparison: AuditComparison }) {
  if (comparison.baselineAuditId === null) {
    return (
      <section className="widget flex items-center gap-3 p-5 text-sm text-muted">
        <GitCompareArrows className="h-5 w-5 shrink-0 text-dim" />
        This is the first completed audit of this website. The next one will show what changed.
      </section>
    )
  }

  const delta = comparison.scoreDelta
  return (
    <section className="widget p-5 sm:p-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-headline text-main">Issues changed since last audit</h2>
          <p className="mt-0.5 text-xs text-dim">
            Compared with{' '}
            <Link to={`/audits/${comparison.baselineAuditId}`} className="font-medium text-accent hover:opacity-75">
              audit #{comparison.baselineAuditId}
            </Link>
            {comparison.baselineCompletedAt ? ` · ${formatDateTime(comparison.baselineCompletedAt)}` : ''}
          </p>
        </div>
        {typeof delta === 'number' && (
          <span
            className={cn(
              'inline-flex items-center gap-1 rounded-full px-3 py-1 text-sm font-semibold font-tabular',
              delta > 0 && 'bg-success-surface text-success',
              delta < 0 && 'bg-critical-surface text-critical',
              delta === 0 && 'bg-surface-elevated text-muted',
            )}
          >
            {delta > 0 ? <ArrowUpRight className="h-4 w-4" /> : delta < 0 ? <ArrowDownRight className="h-4 w-4" /> : null}
            {delta > 0 ? `+${delta}` : delta} score
          </span>
        )}
      </div>

      {!comparison.detailsAvailable ? (
        <p className="mt-4 text-sm text-muted">
          Issue-level details for one of these audits have been removed by data retention, so only scores can be compared.
        </p>
      ) : (
        <>
          <dl className="mt-5 grid grid-cols-3 gap-3 text-center">
            <Stat label="New" value={comparison.newCount} tone={comparison.newCount > 0 ? 'text-critical' : 'text-main'} />
            <Stat label="Fixed" value={comparison.fixedCount} tone={comparison.fixedCount > 0 ? 'text-success' : 'text-main'} />
            <Stat label="Still open" value={comparison.persistingCount} tone="text-main" />
          </dl>
          <div className="mt-5 grid gap-5 lg:grid-cols-2">
            <ChangeList
              title="New issues"
              icon={<PlusCircle className="h-4 w-4 text-critical" />}
              issues={comparison.newIssues}
              total={comparison.newCount}
              empty="No new issues. Nice."
            />
            <ChangeList
              title="Fixed issues"
              icon={<CheckCircle2 className="h-4 w-4 text-success" />}
              issues={comparison.fixedIssues}
              total={comparison.fixedCount}
              empty="Nothing was fixed since the last audit."
            />
          </div>
        </>
      )}
    </section>
  )
}

function Stat({ label, value, tone }: { label: string; value: number; tone: string }) {
  return (
    <div className="rounded-2xl bg-surface-low p-3 dark:bg-surface-elevated/50">
      <dt className="text-xs text-dim">{label}</dt>
      <dd className={cn('mt-0.5 text-2xl font-semibold font-tabular', tone)}>{value}</dd>
    </div>
  )
}

function ChangeList({
  title,
  icon,
  issues,
  total,
  empty,
}: {
  title: string
  icon: ReactNode
  issues: IssueChange[]
  total: number
  empty: string
}) {
  const [expanded, setExpanded] = useState(false)
  const shown = expanded ? issues : issues.slice(0, PREVIEW)

  return (
    <div>
      <h3 className="flex items-center gap-2 text-[13px] font-semibold text-main">
        {icon}
        {title}
      </h3>
      {issues.length === 0 ? (
        <p className="mt-2 text-sm text-dim">{empty}</p>
      ) : (
        <>
          <ul className="mt-2 divide-y divide-default overflow-hidden rounded-2xl border border-default">
            {shown.map((issue) => (
              <li key={issue.fingerprint} className="px-3.5 py-2.5">
                <div className="flex items-center gap-2">
                  <Badge variant={severityVariant(issue.severity)}>{issue.severity.toLowerCase()}</Badge>
                  <span className="truncate text-[13px] font-medium text-main">{issue.ruleTitle || issue.ruleCode}</span>
                </div>
                <p className="mt-1 truncate text-xs text-dim" title={issue.url}>
                  {pathOf(issue.url)}
                </p>
              </li>
            ))}
          </ul>
          {(issues.length > PREVIEW || total > issues.length) && (
            <div className="mt-2 flex items-center justify-between text-xs text-dim">
              {issues.length > PREVIEW ? (
                <button
                  type="button"
                  className="font-semibold text-accent hover:opacity-75"
                  onClick={() => setExpanded((value) => !value)}
                >
                  {expanded ? 'Show less' : `Show all ${issues.length}`}
                </button>
              ) : (
                <span />
              )}
              {total > issues.length && <span>Showing {issues.length} of {total}</span>}
            </div>
          )}
        </>
      )}
    </div>
  )
}
