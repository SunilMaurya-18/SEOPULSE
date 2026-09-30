import { Link } from 'react-router-dom'
import { cn } from '@/lib/cn'
import { useInView } from '@/features/landing/useInView'

export function FinalCtaSection() {
  const { ref, shown } = useInView<HTMLElement>(0.3)

  return (
    <section
      ref={ref}
      className="relative border-b border-white/80 bg-[#05070a] px-5 py-24 md:px-[60px] md:py-32"
    >
      <div
        className="pointer-events-none absolute inset-x-0 top-0 h-64 bg-[radial-gradient(ellipse_at_top,rgba(70,100,120,0.35),transparent_70%)]"
        aria-hidden
      />
      <div className="relative mx-auto grid max-w-6xl items-start gap-10 lg:grid-cols-[1.1fr_auto_0.9fr] lg:gap-12">
        <h2
          className={cn(
            'font-mono text-4xl leading-tight font-normal tracking-wide text-[#f2f5ea] transition duration-700 sm:text-5xl lg:text-6xl',
            shown ? 'opacity-100 blur-none' : 'opacity-40 blur-md',
          )}
        >
          Are you ready to accelerate your site?
        </h2>
        <div className="hidden w-px self-stretch bg-white/50 lg:block" aria-hidden />
        <div>
          <p className="font-mono text-sm leading-relaxed text-[#f2f5ea] sm:text-base">
            Add a site, start an audit, and read the score, the pages, and the
            issues from the same workspace. The report downloads when the run
            is finished.
          </p>
          <Link
            to="/register"
            className="mt-8 inline-flex rounded-md bg-accent px-4 py-2.5 font-mono text-sm text-on-accent transition hover:-translate-y-0.5 hover:brightness-110"
          >
            Get Started
          </Link>
        </div>
      </div>
    </section>
  )
}
