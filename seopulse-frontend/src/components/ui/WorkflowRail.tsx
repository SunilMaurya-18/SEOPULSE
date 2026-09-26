import { Check } from 'lucide-react'
import { Link } from 'react-router-dom'
import { cn } from '@/lib/cn'

export type WorkflowPhase =
  | 'connect'
  | 'crawl'
  | 'analyze'
  | 'report'

const STEPS: {
  id: WorkflowPhase
  label: string
  hint: string
  to?: string
}[] = [
  {
    id: 'connect',
    label: 'Connect',
    hint: 'Add a website',
    to: '/websites',
  },
  {
    id: 'crawl',
    label: 'Crawl',
    hint: 'Run an audit',
    to: '/audits',
  },
  {
    id: 'analyze',
    label: 'Analyze',
    hint: 'Score pages',
  },
  {
    id: 'report',
    label: 'Report',
    hint: 'Review findings',
    to: '/issues',
  },
]

interface WorkflowRailProps {
  phase: WorkflowPhase
  className?: string
  compact?: boolean
}

function phaseIndex(phase: WorkflowPhase) {
  return STEPS.findIndex((step) => step.id === phase)
}

export function WorkflowRail({
  phase,
  className,
  compact = false,
}: WorkflowRailProps) {
  const current = phaseIndex(phase)

  return (
    <nav
      aria-label="Audit workflow"
      className={cn(
        'overflow-hidden rounded-xl border border-default bg-surface',
        className,
      )}
    >
      <ol
        className={cn(
          'grid grid-cols-2 lg:grid-cols-4',
          compact ? 'divide-x divide-default/70' : '',
        )}
      >
        {STEPS.map((step, index) => {
          const done = index < current
          const active = index === current
          const content = (
            <div
              className={cn(
                'relative flex h-full flex-col justify-center px-4 transition-colors',
                compact ? 'py-3' : 'py-4',
                active && 'bg-accent-surface',
                done && 'bg-surface-low/80',
              )}
            >
              <div className="flex items-center gap-2.5">
                <span
                  className={cn(
                    'flex h-6 w-6 items-center justify-center rounded-full border text-[11px] font-semibold font-tabular',
                    done &&
                      'border-success bg-success text-white',
                    active &&
                      'border-accent bg-accent text-white shadow-[0_0_0_4px_var(--sp-accent-surface)]',
                    !done &&
                      !active &&
                      'border-default bg-surface text-dim',
                  )}
                >
                  {done ? <Check className="h-3.5 w-3.5" strokeWidth={2.5} /> : index + 1}
                </span>
                <div className="min-w-0">
                  <p
                    className={cn(
                      'font-display text-sm font-semibold',
                      active ? 'text-accent' : 'text-main',
                    )}
                  >
                    {step.label}
                  </p>
                  {!compact && (
                    <p className="mt-0.5 truncate text-xs text-muted">
                      {step.hint}
                    </p>
                  )}
                </div>
              </div>
              {active && (
                <span className="absolute inset-x-4 bottom-0 h-0.5 bg-accent" />
              )}
            </div>
          )

          if (step.to && (done || active)) {
            return (
              <li key={step.id}>
                <Link to={step.to} className="block h-full hover:bg-surface-elevated/60">
                  {content}
                </Link>
              </li>
            )
          }

          return <li key={step.id}>{content}</li>
        })}
      </ol>
    </nav>
  )
}

export function resolveWorkflowPhase(input: {
  websiteCount: number
  auditCount: number
  hasActiveAudit: boolean
  hasCompletedAudit: boolean
  activeStatus?: string
}): WorkflowPhase {
  if (input.websiteCount === 0) return 'connect'
  if (input.auditCount === 0) return 'crawl'
  if (input.hasActiveAudit) {
    const status = input.activeStatus?.toUpperCase()
    if (status === 'ANALYZING') return 'analyze'
    return 'crawl'
  }
  if (input.hasCompletedAudit) return 'report'
  return 'analyze'
}
