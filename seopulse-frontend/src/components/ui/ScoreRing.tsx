import { useId } from 'react'
import { cn } from '@/lib/cn'

interface ScoreRingProps {
  score: number | null
  size?: number
  strokeWidth?: number
  className?: string
  label?: string
}

const GRADIENTS = {
  good: ['#a6f25b', '#30d158'],
  fair: ['#ffd60a', '#ff9f0a'],
  poor: ['#ff8a5c', '#ff375f'],
} as const

function band(score: number): keyof typeof GRADIENTS {
  if (score >= 80) return 'good'
  if (score >= 60) return 'fair'
  return 'poor'
}

function scoreGrade(score: number) {
  if (score >= 90) return 'A'
  if (score >= 80) return 'B'
  if (score >= 70) return 'C'
  if (score >= 60) return 'D'
  return 'F'
}

export function ScoreRing({ score, size = 120, strokeWidth = 10, className, label = 'score' }: ScoreRingProps) {
  const gradientId = `ring-${useId().replace(/[^a-zA-Z0-9_-]/g, '')}`

  if (score === null) {
    return (
      <div
        className={cn('relative flex flex-col items-center justify-center rounded-full text-dim', className)}
        style={{ width: size, height: size }}
      >
        <svg width={size} height={size} className="absolute inset-0">
          <circle
            cx={size / 2}
            cy={size / 2}
            r={(size - strokeWidth) / 2}
            fill="none"
            stroke="currentColor"
            strokeWidth={strokeWidth}
            className="text-surface-elevated"
          />
        </svg>
        <span className="num text-xl font-semibold">—</span>
        <span className="text-[11px] font-medium">Pending</span>
      </div>
    )
  }

  const safeScore = Math.max(0, Math.min(100, score))
  const radius = (size - strokeWidth) / 2
  const circumference = 2 * Math.PI * radius
  const offset = circumference - (safeScore / 100) * circumference
  const [from, to] = GRADIENTS[band(safeScore)]

  return (
    <div
      className={cn('relative', className)}
      style={{ width: size, height: size }}
      role="img"
      aria-label={`SEO health score ${Math.round(safeScore)} out of 100, grade ${scoreGrade(safeScore)}`}
    >
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="-rotate-90">
        <defs>
          <linearGradient id={gradientId} x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor={from} />
            <stop offset="100%" stopColor={to} />
          </linearGradient>
        </defs>
        <circle cx={size / 2} cy={size / 2} r={radius} fill="none" stroke={to} strokeOpacity={0.16} strokeWidth={strokeWidth} />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke={`url(#${gradientId})`}
          strokeWidth={strokeWidth}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          className="transition-all duration-1000 ease-out"
          style={{ filter: `drop-shadow(0 0 ${strokeWidth / 2}px ${to}55)` }}
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="num text-[28px] leading-none font-bold text-main" style={{ fontSize: size * 0.24 }}>
          {Math.round(safeScore)}
        </span>
        {size >= 90 && (
          <>
            <span className="mt-1 text-[11px] font-medium text-dim capitalize">{label}</span>
            <span className="mt-0.5 text-[11px] font-semibold" style={{ color: to }}>
              Grade {scoreGrade(safeScore)}
            </span>
          </>
        )}
      </div>
    </div>
  )
}
