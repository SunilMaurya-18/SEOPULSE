import { Loader2 } from 'lucide-react'

import type { WebVitals } from '@/api/webVitals'
import { cn } from '@/lib/cn'
import { scoreTone } from '@/lib/format'

type Rating = 'good' | 'average' | 'poor'

/** Google's published thresholds: [good up to, poor above]. */
const thresholds = {
  lcp: [2500, 4000],
  inp: [200, 500],
  cls: [0.1, 0.25],
  fcp: [1800, 3000],
  tbt: [200, 600],
  speedIndex: [3400, 5800],
} as const

const ratingStyles: Record<Rating, { label: string; dot: string; text: string }> = {
  good: { label: 'Good', dot: 'bg-success', text: 'text-success' },
  average: { label: 'Needs work', dot: 'bg-warning', text: 'text-warning' },
  poor: { label: 'Poor', dot: 'bg-critical', text: 'text-critical' },
}

function rate(value: number, [good, poor]: readonly [number, number]): Rating {
  if (value <= good) return 'good'
  if (value <= poor) return 'average'
  return 'poor'
}

function seconds(ms: number) {
  return ms < 1000 ? `${Math.round(ms)} ms` : `${(ms / 1000).toFixed(1)} s`
}

interface Metric {
  key: string
  label: string
  hint: string
  value: number | null
  limits: readonly [number, number]
  format: (value: number) => string
}

export function WebVitalsPanel({ vitals }: { vitals: WebVitals | undefined }) {
  if (!vitals || vitals.state === 'UNAVAILABLE') return null

  const device = vitals.strategy === 'desktop' ? 'Desktop' : 'Mobile'

  return (
    <section className="widget p-5 sm:p-6" aria-live="polite">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-headline text-main">Core Web Vitals</h2>
          <p className="mt-0.5 text-xs text-dim">{device} homepage, measured by Google PageSpeed Insights</p>
        </div>
        {vitals.state === 'READY' && vitals.performanceScore !== null && (
          <div className="text-right">
            <p className={cn('num text-[28px] leading-none font-bold', scoreTone(vitals.performanceScore))}>
              {vitals.performanceScore}
            </p>
            <p className="mt-1 text-xs text-dim">Performance</p>
          </div>
        )}
      </div>

      {vitals.state === 'PENDING' && (
        <p className="mt-5 flex items-center gap-2 text-[13px] text-muted">
          <Loader2 className="h-4 w-4 animate-spin" aria-hidden />
          Measuring page speed. This usually takes under a minute.
        </p>
      )}

      {vitals.state === 'FAILED' && (
        <p className="mt-5 text-[13px] text-muted">
          {vitals.errorMessage ?? 'Page speed could not be measured.'} It will be measured again on the next audit.
        </p>
      )}

      {vitals.state === 'READY' && (
        <>
          {vitals.field && (
            <MetricGroup
              title="Real visitors, last 28 days"
              metrics={[
                { key: 'f-lcp', label: 'Largest Contentful Paint', hint: 'Main content visible', value: vitals.field.lcpMs, limits: thresholds.lcp, format: seconds },
                { key: 'f-inp', label: 'Interaction to Next Paint', hint: 'Response to taps and clicks', value: vitals.field.inpMs, limits: thresholds.inp, format: seconds },
                { key: 'f-cls', label: 'Cumulative Layout Shift', hint: 'Visual stability', value: vitals.field.cls, limits: thresholds.cls, format: (v) => v.toFixed(2) },
              ]}
            />
          )}
          {vitals.lab && (
            <MetricGroup
              title={vitals.field ? 'Lab test' : 'Lab test (not enough real-visitor data yet)'}
              metrics={[
                { key: 'l-lcp', label: 'Largest Contentful Paint', hint: 'Main content visible', value: vitals.lab.lcpMs, limits: thresholds.lcp, format: seconds },
                { key: 'l-cls', label: 'Cumulative Layout Shift', hint: 'Visual stability', value: vitals.lab.cls, limits: thresholds.cls, format: (v) => v.toFixed(2) },
                { key: 'l-tbt', label: 'Total Blocking Time', hint: 'Stands in for responsiveness', value: vitals.lab.tbtMs, limits: thresholds.tbt, format: seconds },
                { key: 'l-fcp', label: 'First Contentful Paint', hint: 'First thing drawn', value: vitals.lab.fcpMs, limits: thresholds.fcp, format: seconds },
                { key: 'l-si', label: 'Speed Index', hint: 'How fast the page fills in', value: vitals.lab.speedIndexMs, limits: thresholds.speedIndex, format: seconds },
              ]}
            />
          )}
        </>
      )}
    </section>
  )
}

function MetricGroup({ title, metrics }: { title: string; metrics: Metric[] }) {
  const shown = metrics.filter((metric) => metric.value !== null)
  if (shown.length === 0) return null
  return (
    <div className="mt-5">
      <h3 className="text-xs font-semibold tracking-[0.04em] text-dim uppercase">{title}</h3>
      <ul className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {shown.map((metric) => {
          const value = metric.value as number
          const rating = ratingStyles[rate(value, metric.limits)]
          return (
            <li key={metric.key} className="rounded-xl bg-surface-low p-3.5 dark:bg-surface-elevated/60">
              <p className="text-[13px] font-medium text-main">{metric.label}</p>
              <p className="text-xs text-dim">{metric.hint}</p>
              <div className="mt-2 flex items-baseline justify-between gap-2">
                <span className="num text-xl font-bold text-main">{metric.format(value)}</span>
                <span className={cn('flex items-center gap-1.5 text-xs font-semibold', rating.text)}>
                  <span className={cn('h-2 w-2 rounded-full', rating.dot)} aria-hidden />
                  {rating.label}
                </span>
              </div>
            </li>
          )
        })}
      </ul>
    </div>
  )
}
