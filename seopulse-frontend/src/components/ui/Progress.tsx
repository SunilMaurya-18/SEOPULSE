import { cn } from '@/lib/cn'

interface ProgressProps {
  value: number
  max?: number
  label?: string
  meta?: string
  className?: string
  tone?: 'accent' | 'success' | 'warning' | 'critical' | 'info'
}

const tones = {
  accent: 'bg-accent',
  success: 'bg-success',
  warning: 'bg-warning',
  critical: 'bg-critical',
  info: 'bg-info',
}

export function Progress({
  value,
  max = 100,
  label,
  meta,
  className,
  tone = 'accent',
}: ProgressProps) {
  const pct = Math.max(0, Math.min(100, (value / max) * 100))

  return (
    <div className={cn('space-y-1.5', className)}>
      {(label || meta) && (
        <div className="flex items-center justify-between gap-2">
          {label && (
            <span className="font-mono text-[11px] tracking-wide text-muted uppercase">
              {label}
            </span>
          )}
          {meta && (
            <span className="font-mono text-[11px] text-dim font-tabular">
              {meta}
            </span>
          )}
        </div>
      )}
      <div
        className="h-1.5 w-full overflow-hidden rounded bg-surface-elevated"
        role="progressbar"
        aria-valuenow={Math.round(pct)}
        aria-valuemin={0}
        aria-valuemax={100}
      >
        <div
          className={cn('h-full rounded transition-all duration-500', tones[tone])}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  )
}
