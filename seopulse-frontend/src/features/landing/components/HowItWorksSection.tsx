import { Link } from 'react-router-dom'

export function HowItWorksSection() {
  const steps = [
    {
      step: '01',
      title: 'Connect a website',
      copy: 'Paste a URL. SEOPulse validates the host and attaches it to your workspace.',
    },
    {
      step: '02',
      title: 'Crawl and analyze',
      copy: 'Watch live stages — queued, crawling, analyzing — while pages are scored.',
    },
    {
      step: '03',
      title: 'Ship the fixes',
      copy: 'Open ranked issues, inspect URLs, and track health after every audit.',
    },
  ]

  return (
    <section className="border-b border-default bg-canvas px-4 py-20 sm:px-6">
      <div className="mx-auto w-full max-w-5xl">
        <p className="font-mono text-[11px] tracking-[0.16em] text-accent uppercase">
          Workflow
        </p>
        <h2 className="mt-3 font-display text-3xl font-semibold tracking-tight text-main sm:text-4xl">
          From URL to report in one pass
        </h2>
        <p className="mt-3 max-w-2xl text-base text-muted">
          The product flow stays intentional: connect, crawl, then act on ranked
          findings — without bouncing between tools.
        </p>

        <ol className="mt-12 grid gap-0 overflow-hidden rounded-2xl border border-default bg-surface md:grid-cols-3">
          {steps.map((item, index) => (
            <li
              key={item.step}
              className={
                index < steps.length - 1
                  ? 'border-b border-default md:border-r md:border-b-0'
                  : undefined
              }
            >
              <div className="h-full px-6 py-8">
                <p className="font-mono text-xs tracking-wider text-accent">
                  {item.step}
                </p>
                <h3 className="mt-4 font-display text-xl font-semibold text-main">
                  {item.title}
                </h3>
                <p className="mt-2 text-sm leading-6 text-muted">{item.copy}</p>
              </div>
            </li>
          ))}
        </ol>

        <p className="mt-8 text-sm text-muted">
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-accent hover:text-accent-hover">
            Sign in and continue
          </Link>
        </p>
      </div>
    </section>
  )
}
