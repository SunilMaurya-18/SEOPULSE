import { useMemo, useState } from 'react'
import { AlertCircle, AlertTriangle, ChevronDown, ExternalLink, Info, Lightbulb } from 'lucide-react'

import type { SeoIssue } from '@/api/audits'
import { IconTile } from '@/components/ui/IconTile'
import type { Tint } from '@/components/ui/tints'
import { cn } from '@/lib/cn'
import { pathOf } from '@/lib/format'

interface IssueGroup {
  ruleCode: string
  severity: string
  message: string
  recommendation: string
  urls: string[]
}

const SEVERITY_ORDER: Record<string, number> = { CRITICAL: 0, ERROR: 0, WARNING: 1, WARN: 1, INFO: 2 }

function severityStyle(severity: string): { tint: Tint; icon: typeof AlertCircle; label: string; text: string } {
  const normalized = severity.toUpperCase()
  if (normalized === 'ERROR' || normalized === 'CRITICAL') {
    return { tint: 'red', icon: AlertCircle, label: 'Error', text: 'text-critical' }
  }
  if (normalized === 'WARNING' || normalized === 'WARN') {
    return { tint: 'orange', icon: AlertTriangle, label: 'Warning', text: 'text-warning' }
  }
  return { tint: 'blue', icon: Info, label: 'Notice', text: 'text-info' }
}

function groupIssues(issues: SeoIssue[]): IssueGroup[] {
  const groups = new Map<string, IssueGroup>()
  for (const issue of issues) {
    const existing = groups.get(issue.ruleCode)
    if (existing) {
      if (!existing.urls.includes(issue.url)) existing.urls.push(issue.url)
    } else {
      groups.set(issue.ruleCode, {
        ruleCode: issue.ruleCode,
        severity: issue.severity,
        message: issue.message,
        recommendation: issue.recommendation,
        urls: [issue.url],
      })
    }
  }
  return [...groups.values()].sort(
    (a, b) =>
      (SEVERITY_ORDER[a.severity.toUpperCase()] ?? 3) - (SEVERITY_ORDER[b.severity.toUpperCase()] ?? 3) ||
      b.urls.length - a.urls.length,
  )
}

export function IssueGroups({ issues }: { issues: SeoIssue[] }) {
  const groups = useMemo(() => groupIssues(issues), [issues])
  return (
    <ul className="space-y-3">
      {groups.map((group) => (
        <IssueGroupRow key={group.ruleCode} group={group} />
      ))}
    </ul>
  )
}

function IssueGroupRow({ group }: { group: IssueGroup }) {
  const [open, setOpen] = useState(false)
  const style = severityStyle(group.severity)
  const Icon = style.icon
  const single = group.urls.length === 1

  return (
    <li className="widget overflow-hidden">
      <div className="flex gap-4 p-5">
        <IconTile tint={style.tint} size="lg">
          <Icon />
        </IconTile>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className={cn('text-xs font-semibold', style.text)}>{style.label}</span>
            <span className="rounded-full bg-surface-low px-2 py-0.5 text-[11px] font-medium text-dim font-tabular dark:bg-surface-elevated/70">
              {group.ruleCode}
            </span>
          </div>
          <p className="mt-1.5 text-[15px] font-semibold text-main">{group.message}</p>
          {group.recommendation && (
            <p className="mt-2 flex gap-2 text-sm leading-relaxed text-muted">
              <Lightbulb className="mt-0.5 h-4 w-4 shrink-0 text-warning" />
              {group.recommendation}
            </p>
          )}
          {single && (
            <a
              href={group.urls[0]}
              target="_blank"
              rel="noreferrer"
              className="mt-3 inline-flex max-w-full items-center gap-1.5 rounded-full bg-surface-low px-3 py-1 text-xs font-medium text-muted transition-colors hover:text-accent dark:bg-surface-elevated/60"
              title={group.urls[0]}
            >
              <span className="truncate">{group.urls[0]}</span>
              <ExternalLink className="h-3 w-3 shrink-0" />
            </a>
          )}
        </div>
        {!single && (
          <button
            type="button"
            onClick={() => setOpen((value) => !value)}
            aria-expanded={open}
            className="flex h-8 shrink-0 items-center gap-1 self-start rounded-full bg-surface-elevated px-3 text-[13px] font-semibold text-main transition-colors hover:bg-surface-high"
          >
            {group.urls.length} pages
            <ChevronDown className={cn('h-4 w-4 transition-transform', open && 'rotate-180')} />
          </button>
        )}
      </div>
      {!single && open && (
        <ul className="divide-y divide-default border-t border-default bg-surface-low/60 dark:bg-surface-elevated/25">
          {group.urls.map((url) => (
            <li key={url}>
              <a
                href={url}
                target="_blank"
                rel="noreferrer"
                title={url}
                className="flex items-center gap-2 px-5 py-2.5 pl-[76px] text-xs text-muted transition-colors hover:text-accent"
              >
                <span className="truncate">{pathOf(url)}</span>
                <ExternalLink className="h-3 w-3 shrink-0" />
              </a>
            </li>
          ))}
        </ul>
      )}
    </li>
  )
}