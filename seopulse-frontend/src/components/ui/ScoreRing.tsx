import { cn } from '@/lib/cn'

interface ScoreRingProps {
  score: number | null
  size?: number
  strokeWidth?: number
  className?: string
  label?: string
}

function scoreTone(score: number) {
  if (score >= 80) return 'text-success'
  if (score >= 60) return 'text-warning'
  return 'text-critical'
}

function scoreGrade(score: number) {
  if (score >= 90) return 'A'
  if (score >= 80) return 'B'
  if (score >= 70) return 'C'
  if (score >= 60) return 'D'
  return 'F'
}

export function ScoreRing({
  score,
  size = 120,
  strokeWidth = 10,
  className,
  label = 'score',
}: ScoreRingProps) {
  if (score === null) {
    return (
      <div
        className={cn(
          'flex flex-col items-center justify-center rounded-full border border-dashed border-default text-dim',
          className,
        )}
        style={{ width: size, height: size }}
      >
        <span className="font-display text-xl font-semibold">—</span>
        <span className="font-mono text-[10px] tracking-wider uppercase">
          pending
        </span>
      </div>
    )
  }

  const safeScore = Math.max(0, Math.min(100, score))
  const radius = (size - strokeWidth) / 2
  const circumference = 2 * Math.PI * radius
  const offset = circumference - (safeScore / 100) * circumference
  const tone = scoreTone(safeScore)

  return (
    <div
      className={cn('relative', className)}
      style={{ width: size, height: size }}
      role="img"
      aria-label={`SEO health score ${Math.round(safeScore)} out of 100, grade ${scoreGrade(safeScore)}`}
    >
      <svg
        width={size}
        height={size}
        viewBox={`0 0 ${size} ${size}`}
        className="-rotate-90"
      >
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth={strokeWidth}
          className="text-surface-high"
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth={strokeWidth}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          className={cn('transition-all duration-700', tone)}
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="font-display text-2xl font-semibold tracking-tight text-main font-tabular">
          {Math.round(safeScore)}
        </span>
        <span className="font-mono text-[10px] tracking-wider text-dim uppercase">
          {label}
        </span>
        <span className={cn('mt-0.5 font-mono text-[10px] font-medium', tone)}>
          Grade {scoreGrade(safeScore)}
        </span>
      </div>
    </div>
  )
}
