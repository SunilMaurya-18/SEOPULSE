import type { ReactNode } from 'react'

import { MarketingFooter } from '@/features/landing/components/MarketingFooter'
import { MarketingHeader } from '@/features/landing/components/MarketingHeader'
import { contactEmail } from '@/lib/contact'

export function LegalLayout({
  eyebrow,
  title,
  updated,
  children,
}: {
  eyebrow: string
  title: string
  updated: string
  children: ReactNode
}) {
  return (
    <div className="marketing dark min-h-dvh bg-black text-main">
      <MarketingHeader />
      <main className="mx-auto max-w-3xl px-5 pt-36 pb-24 md:px-[60px]">
        <p className="font-mono text-xs tracking-[0.22em] text-accent uppercase">{eyebrow}</p>
        <h1 className="mt-3 font-mono text-4xl leading-[1.08] font-semibold tracking-tight sm:text-5xl">{title}</h1>
        <p className="mt-4 font-mono text-sm text-muted">Last updated {updated}</p>
        <div className="mt-12 space-y-10">{children}</div>
      </main>
      <MarketingFooter />
    </div>
  )
}

export function LegalSection({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section>
      <h2 className="font-mono text-xl font-semibold tracking-tight">{title}</h2>
      <div className="mt-4 space-y-4 text-[15px] leading-relaxed text-muted [&_a]:text-main [&_a]:underline [&_a]:underline-offset-4 [&_li]:pl-1 [&_strong]:text-main [&_ul]:list-disc [&_ul]:space-y-2 [&_ul]:pl-5">
        {children}
      </div>
    </section>
  )
}

export function ContactLink() {
  return <a href={`mailto:${contactEmail}`}>{contactEmail}</a>
}
