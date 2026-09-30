import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'

import { MarketingFooter } from '@/features/landing/components/MarketingFooter'
import { MarketingHeader } from '@/features/landing/components/MarketingHeader'

const plans = [
  {
    code: 'FREE',
    name: 'Free',
    monthly: { usd: 0, inr: 0 },
    yearly: { usd: 0, inr: 0 },
    points: ['1 website', '100 pages per audit', '5 audits a month', '1 member'],
  },
  {
    code: 'PRO',
    name: 'Pro',
    monthly: { usd: 29, inr: 1999 },
    yearly: { usd: 290, inr: 19990 },
    points: ['10 websites', '2,000 pages per audit', '100 audits a month', '3 members', '14-day trial'],
  },
  {
    code: 'AGENCY',
    name: 'Agency',
    monthly: { usd: 99, inr: 6999 },
    yearly: { usd: 990, inr: 69990 },
    points: ['50 websites', '10,000 pages per audit', '1,000 audits a month', '15 members', 'API access later'],
  },
]

export function PricingPage() {
  const [yearly, setYearly] = useState(false)
  const inr = useMemo(() => {
    const locale = navigator.language.toLowerCase()
    return locale.includes('in') || locale.startsWith('hi')
  }, [])

  return (
    <div className="dark min-h-dvh bg-black text-[#f2f5ea]">
      <MarketingHeader />
      <main className="mx-auto max-w-6xl px-5 pt-36 pb-24 md:px-[60px]">
        <p className="font-mono text-xs tracking-[0.22em] text-accent uppercase">Pricing</p>
        <h1 className="mt-3 max-w-xl font-mono text-4xl leading-tight sm:text-6xl">Plans that stay out of the way.</h1>
        <div className="mt-8 inline-flex rounded-full border border-white/15 p-1 font-mono text-sm">
          <button type="button" className={!yearly ? 'rounded-full bg-accent px-4 py-2 text-on-accent' : 'px-4 py-2 text-[#8b93a1]'} onClick={() => setYearly(false)}>
            Monthly
          </button>
          <button type="button" className={yearly ? 'rounded-full bg-accent px-4 py-2 text-on-accent' : 'px-4 py-2 text-[#8b93a1]'} onClick={() => setYearly(true)}>
            Yearly
          </button>
        </div>
        <div className="mt-10 grid gap-4 lg:grid-cols-3">
          {plans.map((plan) => {
            const price = yearly ? plan.yearly : plan.monthly
            const amount = inr ? price.inr : price.usd
            return (
              <article key={plan.code} className="rounded-2xl border border-white/10 bg-[#0c1016] p-6">
                <h2 className="font-mono text-xl">{plan.name}</h2>
                <p className="mt-4 font-mono text-4xl">
                  {inr ? '₹' : '$'}
                  {amount.toLocaleString()}
                  <span className="text-base text-[#8b93a1]">{plan.code === 'FREE' ? '' : yearly ? '/yr' : '/mo'}</span>
                </p>
                <ul className="mt-6 space-y-2 text-sm text-[#c5c9c2]">
                  {plan.points.map((point) => (
                    <li key={point}>{point}</li>
                  ))}
                </ul>
                <Link
                  to={plan.code === 'FREE' ? '/register' : '/settings'}
                  className="mt-8 inline-flex rounded-md bg-accent px-4 py-2 font-mono text-sm text-on-accent"
                >
                  {plan.code === 'FREE' ? 'Create account' : 'Upgrade'}
                </Link>
              </article>
            )
          })}
        </div>
      </main>
      <MarketingFooter />
    </div>
  )
}
