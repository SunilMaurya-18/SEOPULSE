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
  accent: 'from-[#ff8a65] to-accent',
  success: 'from-[#a6f25b] to-success',
  warning: 'from-[#ffd60a] to-warning',
  critical: 'from-[#ff8a5c] to-critical',
  info: 'from-[#5ac8fa] to-info',
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
        <div className="flex items-center justify-between gap-2 text-xs">
          {label && <span className="font-medium text-muted">{label}</span>}
          {meta && <span className="font-semibold text-dim font-tabular">{meta}</span>}
        </div>
      )}
      <div
        className="h-1.5 w-full overflow-hidden rounded-full bg-surface-elevated"
        role="progressbar"
        aria-valuenow={Math.round(pct)}
        aria-valuemin={0}
        aria-valuemax={100}
      >
        <div
          className={cn('h-full rounded-full bg-gradient-to-r transition-all duration-700 ease-out', tones[tone])}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  )
}
