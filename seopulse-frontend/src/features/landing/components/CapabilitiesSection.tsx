export function CapabilitiesSection() {
  const items = [
    {
      title: 'Live crawl telemetry',
      copy: 'Watch queued, crawling, and analyzing states with page throughput as audits run.',
    },
    {
      title: 'Ranked SEO issues',
      copy: 'Errors, warnings, and info findings mapped to URLs with clear recommendations.',
    },
    {
      title: 'Page inventory',
      copy: 'Inspect titles, meta, H1s, canonicals, status codes, and link counts per URL.',
    },
    {
      title: 'Workspace health score',
      copy: 'Dashboard KPIs update as websites and audits complete — not as static screenshots.',
    },
  ]

  return (
    <section className="border-b border-default bg-surface-low px-4 py-20 sm:px-6">
      <div className="mx-auto w-full max-w-5xl">
        <p className="font-mono text-[11px] tracking-[0.16em] text-accent uppercase">
          Platform
        </p>
        <h2 className="mt-3 font-display text-3xl font-semibold tracking-tight text-main sm:text-4xl">
          Built for operators, not slide decks
        </h2>
        <p className="mt-3 max-w-2xl text-base text-muted">
          Every surface in the app maps to a real API response — websites,
          audits, issues, and pages stay in sync after each action.
        </p>

        <div className="mt-12 grid gap-x-10 gap-y-12 sm:grid-cols-2">
          {items.map((item) => (
            <div key={item.title}>
              <h3 className="font-display text-lg font-semibold text-main">
                {item.title}
              </h3>
              <p className="mt-2 text-sm leading-6 text-muted">{item.copy}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
