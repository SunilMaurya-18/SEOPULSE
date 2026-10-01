import { useMemo } from 'react'
import { Link } from 'react-router-dom'

import type { TrendPoint } from '@/api/audits'
import { formatDateTime } from '@/lib/format'

const WIDTH = 640
const HEIGHT = 180
const PAD_X = 28
const PAD_Y = 16

interface Plotted {
  point: TrendPoint
  x: number
  y: number
}

export function TrendChart({ points, currentAuditId }: { points: TrendPoint[]; currentAuditId?: number }) {
  const plotted = useMemo<Plotted[]>(() => {
    const scored = points.filter((point) => typeof point.score === 'number')
    if (scored.length === 0) return []
    const step = scored.length === 1 ? 0 : (WIDTH - PAD_X * 2) / (scored.length - 1)
    return scored.map((point, index) => ({
      point,
      x: scored.length === 1 ? WIDTH / 2 : PAD_X + index * step,
      y: PAD_Y + ((100 - (point.score as number)) / 100) * (HEIGHT - PAD_Y * 2),
    }))
  }, [points])

  if (plotted.length < 2) {
    return (
      <p className="py-8 text-center text-sm text-dim">
        The trend appears after two completed audits of this website.
      </p>
    )
  }

  const line = plotted.map(({ x, y }, index) => `${index === 0 ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`).join(' ')
  const area = `${line} L${plotted[plotted.length - 1].x.toFixed(1)},${HEIGHT - PAD_Y} L${plotted[0].x.toFixed(1)},${HEIGHT - PAD_Y} Z`
  const first = plotted[0].point
  const last = plotted[plotted.length - 1].point

  return (
    <figure>
      <svg
        viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
        className="h-auto w-full"
        role="img"
        aria-label={`Score trend over ${plotted.length} audits, from ${first.score} to ${last.score}`}
      >
        {[0, 50, 100].map((value) => {
          const y = PAD_Y + ((100 - value) / 100) * (HEIGHT - PAD_Y * 2)
          return (
            <g key={value}>
              <line x1={PAD_X} x2={WIDTH - PAD_X} y1={y} y2={y} className="stroke-[var(--sp-border)]" strokeDasharray="3 4" />
              <text x={4} y={y + 4} className="fill-[var(--sp-dim)] text-[10px]">
                {value}
              </text>
            </g>
          )
        })}
        <path d={area} className="fill-accent/10" />
        <path d={line} fill="none" className="stroke-accent" strokeWidth={2.5} strokeLinejoin="round" strokeLinecap="round" />
        {plotted.map(({ point, x, y }) => {
          const current = point.auditId === currentAuditId
          return (
            <Link key={point.auditId} to={`/audits/${point.auditId}`} aria-label={`Audit #${point.auditId}, score ${point.score}`}>
              <circle
                cx={x}
                cy={y}
                r={current ? 6 : 4}
                className={current ? 'fill-accent stroke-[var(--sp-surface)]' : 'fill-[var(--sp-surface)] stroke-accent'}
                strokeWidth={2}
              >
                <title>
                  {`Score ${point.score} · ${formatDateTime(point.completedAt)}${point.triggeredBy === 'SCHEDULED' ? ' · scheduled' : ''}`}
                </title>
              </circle>
            </Link>
          )
        })}
      </svg>
      <figcaption className="mt-2 flex justify-between text-xs text-dim">
        <span>{formatDateTime(first.completedAt)}</span>
        <span>{formatDateTime(last.completedAt)}</span>
      </figcaption>
    </figure>
  )
}
