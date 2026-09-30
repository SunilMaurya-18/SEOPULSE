import { Check } from 'lucide-react'
import { Link } from 'react-router-dom'
import { cn } from '@/lib/cn'

export type WorkflowPhase = 'connect' | 'crawl' | 'analyze' | 'report'

const STEPS: {
  id: WorkflowPhase
  label: string
  hint: string
  to?: string
}[] = [
  { id: 'connect', label: 'Connect', hint: 'Add a website', to: '/websites' },
  { id: 'crawl', label: 'Crawl', hint: 'Run an audit', to: '/audits' },
  { id: 'analyze', label: 'Analyze', hint: 'Score pages' },
  { id: 'report', label: 'Report', hint: 'Review findings', to: '/issues' },
]

interface WorkflowRailProps {
  phase: WorkflowPhase
  className?: string
  compact?: boolean
}

function phaseIndex(phase: WorkflowPhase) {
  return STEPS.findIndex((step) => step.id === phase)
}

export function WorkflowRail({ phase, className, compact = false }: WorkflowRailProps) {
  const current = phaseIndex(phase)
  const progress = (current / (STEPS.length - 1)) * 100

  return (
    <nav aria-label="Audit workflow" className={cn('widget px-5 sm:px-6', compact ? 'py-4' : 'py-5', className)}>
      <div className="relative">
        <div className="absolute top-[13px] right-[12.5%] left-[12.5%] hidden h-[3px] rounded-full bg-surface-elevated lg:block">
          <div
            className="h-full rounded-full bg-gradient-to-r from-success to-accent transition-all duration-700"
            style={{ width: `${progress}%` }}
          />
        </div>
        <ol className="relative grid grid-cols-2 gap-y-4 lg:grid-cols-4">
          {STEPS.map((step, index) => {
            const done = index < current
            const active = index === current
            const content = (
              <div className="flex flex-col items-center text-center">
                <span
                  className={cn(
                    'relative flex h-7 w-7 items-center justify-center rounded-full text-[12px] font-semibold font-tabular transition-all',
                    done && 'bg-success text-white',
                    active && 'bg-accent text-white shadow-[0_0_0_5px_var(--sp-accent-surface)]',
                    !done && !active && 'bg-surface-elevated text-dim',
                  )}
                >
                  {done ? <Check className="h-3.5 w-3.5" strokeWidth={3} /> : index + 1}
                </span>
                <p className={cn('mt-2.5 text-[13px] font-semibold', active ? 'text-main' : done ? 'text-main' : 'text-dim')}>
                  {step.label}
                </p>
                {!compact && <p className="mt-0.5 text-xs text-dim">{step.hint}</p>}
              </div>
            )

            if (step.to && (done || active)) {
              return (
                <li key={step.id}>
                  <Link to={step.to} className="block rounded-2xl transition-opacity hover:opacity-80">
                    {content}
                  </Link>
                </li>
              )
            }

            return <li key={step.id}>{content}</li>
          })}
        </ol>
      </div>
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
