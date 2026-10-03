import type { ReactNode } from 'react'

import { MarketingFooter } from '@/features/landing/components/MarketingFooter'
import { MarketingHeader } from '@/features/landing/components/MarketingHeader'

const ROBOTS_EXAMPLE = `User-agent: SEOPulseBot
Disallow: /

# or slow it down:
User-agent: SEOPulseBot
Crawl-delay: 10`

export function BotPage() {
  return (
    <div className="dark min-h-screen bg-canvas font-mono text-sm text-main antialiased">
      <MarketingHeader />
      <main className="mx-auto w-full max-w-3xl px-4 pt-28 pb-20 sm:px-6">
        <p className="font-mono text-[11px] tracking-wider text-accent uppercase">
          Crawler information
        </p>
        <h1 className="mt-2 font-display text-3xl font-semibold text-main">
          SEOPulseBot
        </h1>
        <p className="mt-4 text-base text-muted">
          SEOPulseBot is the crawler behind SEOPulse SEO audits. It visits a
          site only when a SEOPulse user starts an audit for it, and it reads
          public HTML pages to report on titles, headings, links, images and
          HTTP status codes.
        </p>

        <Section title="How it identifies itself">
          <p>Requests carry this user agent:</p>
          <pre className="mt-3 overflow-x-auto rounded-lg border border-default bg-surface p-4 font-mono text-xs text-main">
            SEOPulseBot/1.0 (+{window.location.origin}/bot)
          </pre>
        </Section>

        <Section title="How it behaves">
          <ul className="list-disc space-y-2 pl-5">
            <li>
              It follows <code className="font-mono">robots.txt</code> (RFC
              9309), including <code className="font-mono">Crawl-delay</code>{' '}
              (capped at 30 seconds). If robots.txt cannot be fetched because
              of a server error, it does not crawl the site.
            </li>
            <li>
              It spaces requests to each host and backs off when a server
              responds with 429 or 503, honouring{' '}
              <code className="font-mono">Retry-After</code>.
            </li>
            <li>
              It stays on the audited site (the domain and its{' '}
              <code className="font-mono">www.</code> variant), reads sitemaps
              listed in robots.txt, and stops after a fixed page and time
              budget.
            </li>
            <li>
              It only requests public addresses and never submits forms or
              logs in.
            </li>
          </ul>
        </Section>

        <Section title="How to control or block it">
          <p>Add a rule for SEOPulseBot to your robots.txt:</p>
          <pre className="mt-3 overflow-x-auto rounded-lg border border-default bg-surface p-4 font-mono text-xs text-main">
            {ROBOTS_EXAMPLE}
          </pre>
          <p className="mt-3">
            Changes are picked up within 24 hours, since robots.txt is cached
            for that long.
          </p>
        </Section>
      </main>
      <MarketingFooter />
    </div>
  )
}

function Section({
  title,
  children,
}: {
  title: string
  children: ReactNode
}) {
  return (
    <section className="mt-10">
      <h2 className="font-display text-lg font-semibold text-main">{title}</h2>
      <div className="mt-3 text-sm leading-relaxed text-muted">{children}</div>
    </section>
  )
}
