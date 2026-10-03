import { useEffect, useState } from 'react'
import { useInView, useReducedMotion } from '@/features/landing/useInView'

const stats = [
  { value: 500, suffix: '', label: 'Pages in a crawl' },
  { value: 7, suffix: '', label: 'On-page checks' },
  { value: 100, suffix: '', label: 'Point score' },
  { value: 2, suffix: '', label: 'Report formats' },
  { value: 1, suffix: '', label: 'Timeline for the run' },
]

function useCount(target: number, active: boolean) {
  const reduced = useReducedMotion()
  const [value, setValue] = useState(0)

  useEffect(() => {
    if (!active || reduced) return
    const start = performance.now()
    const duration = 1100
    let raf = 0
    const tick = (now: number) => {
      const progress = Math.min(1, (now - start) / duration)
      const eased = 1 - (1 - progress) ** 3
      setValue(Math.round(target * eased))
      if (progress < 1) raf = window.requestAnimationFrame(tick)
    }
    raf = window.requestAnimationFrame(tick)
    return () => window.cancelAnimationFrame(raf)
  }, [active, reduced, target])

  if (reduced && active) return target
  return value
}

function Stat({
  value,
  suffix,
  label,
  active,
}: {
  value: number
  suffix: string
  label: string
  active: boolean
}) {
  const current = useCount(value, active)
  return (
    <div className="min-w-0 text-center">
      <p className="font-mono text-5xl font-semibold tracking-tight text-accent tabular-nums sm:text-6xl">
        {current}
        {suffix}
      </p>
      <p className="mt-3 font-mono text-sm text-[#f2f5ea]">{label}</p>
    </div>
  )
}

export function StatsSection() {
  const { ref, shown } = useInView<HTMLElement>(0.35)

  return (
    <section
      ref={ref}
      className="relative overflow-hidden bg-[#05070a] px-5 py-24 md:px-[60px] md:py-32"
    >
      <div className="starfield pointer-events-none absolute inset-0 opacity-80" aria-hidden />
      <div className="relative mx-auto max-w-6xl">
        <h2 className="text-center font-mono text-3xl font-semibold tracking-tight text-[#f2f5ea] sm:text-5xl">
          We take pride in the numbers that describe a run.
        </h2>
        <div className="mt-16 grid grid-cols-2 gap-10 sm:grid-cols-3 lg:grid-cols-5">
          {stats.map((stat) => (
            <Stat key={stat.label} {...stat} active={shown} />
          ))}
        </div>
      </div>
    </section>
  )
}
