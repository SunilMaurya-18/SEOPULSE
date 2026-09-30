import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { cn } from '@/lib/cn'
import { useInView, useReducedMotion } from '@/features/landing/useInView'

const lines = [
  'Audits move from the queue to a scored report without a second tool.',
  'The crawl, the issues, and the page list stay on one timeline.',
  'When a run fails, you see why, and you can stop one that should not continue.',
]

export function VisionSection() {
  const { ref, shown } = useInView<HTMLElement>(0.35)
  const reduced = useReducedMotion()
  const [count, setCount] = useState(0)
  const visibleLines = reduced ? lines.length : count

  useEffect(() => {
    if (!shown || reduced) return
    let current = 0
    const timer = window.setInterval(() => {
      current += 1
      setCount(current)
      if (current >= lines.length) window.clearInterval(timer)
    }, 700)
    return () => window.clearInterval(timer)
  }, [shown, reduced])

  return (
    <section
      id="vision"
      ref={ref}
      className="scroll-mt-24 bg-[#05070a] px-5 py-24 md:px-[60px] md:py-32"
    >
      <div className="mx-auto grid max-w-6xl items-start gap-12 lg:grid-cols-2 lg:gap-20">
        <h2
          className={cn(
            'font-mono text-4xl leading-tight font-normal tracking-wide text-[#f2f5ea] transition duration-700 sm:text-5xl lg:text-6xl',
            shown ? 'translate-y-0 opacity-100 blur-none' : 'translate-y-3 opacity-40 blur-md',
          )}
        >
          Unprecedented velocity. Impeccable reliability.
        </h2>
        <div>
          <p className="font-mono text-sm leading-relaxed text-[#f2f5ea] sm:text-base">
            <span className="caret-blink mr-2 inline-block h-4 w-2 translate-y-0.5 bg-accent align-middle" aria-hidden />
            {lines.slice(0, visibleLines).join(' ')}
          </p>
          <Link
            to="/register"
            className={cn(
              'mt-8 inline-flex rounded-md bg-accent px-4 py-2.5 font-mono text-sm text-on-accent transition hover:-translate-y-0.5 hover:brightness-110',
              visibleLines >= lines.length ? 'opacity-100' : 'pointer-events-none opacity-0',
            )}
          >
            Learn More
          </Link>
        </div>
      </div>
    </section>
  )
}
