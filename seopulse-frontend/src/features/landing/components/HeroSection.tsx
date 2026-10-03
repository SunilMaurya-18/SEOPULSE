import { Download, Gauge, ScanSearch } from 'lucide-react'

import { QuickCheck } from '@/features/landing/components/QuickCheck'
import { smoothScrollTo } from '@/features/landing/useInView'
import { PredictiveArcCanvas, TextAnimationCollection } from '@designcodeio/threeui'
import '@designcodeio/threeui/style.css'

const features = [
  { label: 'Crawl every page', icon: ScanSearch },
  { label: 'Score every issue', icon: Gauge },
  { label: 'Download the report', icon: Download },
]

const stats = ['10k+ pages crawled', '50+ SEO checks', '< 60s average scan', 'PDF and CSV export']

export function HeroSection() {
  return (
    <section className="relative isolate flex min-h-dvh flex-col overflow-hidden bg-black">
      <div className="shader-frame pointer-events-none absolute -top-[14%] right-0 left-0 z-0 h-[118%]" aria-hidden>
        <PredictiveArcCanvas
          mode="dark"
          speed={1}
          hue={108}
          saturation={0}
          brightness={1}
          archHeight={0.85}
          thickness={1.15}
          className="h-full w-full"
        />
      </div>
      <div
        className="pointer-events-none absolute inset-0 z-[1] bg-[radial-gradient(ellipse_at_50%_38%,transparent_0%,transparent_46%,rgba(3,3,3,0.55)_100%)]"
        aria-hidden
      />
      <div
        className="pointer-events-none absolute inset-x-0 bottom-0 z-[1] h-32 bg-gradient-to-b from-transparent to-black"
        aria-hidden
      />

      <div className="relative z-10 flex flex-1 flex-col px-5 pt-[168px] pb-16 md:px-[60px] md:pt-[192px]">
        <p className="hero-rise font-mono text-xs tracking-[0.22em] text-accent uppercase">
          SEO analytics platform
        </p>

        <h1 className="sr-only">SEOPulse</h1>
        <div className="shader-frame hero-rise hero-rise-2 pointer-events-none mt-3 h-[clamp(120px,16vw,210px)] w-full" aria-hidden>
          <TextAnimationCollection
            variant="threeui-intro"
            mode="dark"
            hue={0}
            saturation={0}
            brightness={1}
          />
        </div>

        <p className="hero-rise hero-rise-3 mt-4 font-mono text-lg text-main sm:text-2xl">
          Use data to get a 360-degree view of your site.
        </p>
        <p className="hero-rise hero-rise-4 mt-4 max-w-[720px] font-mono text-base leading-[1.7] text-muted sm:text-lg">
          SEOPulse is a website audit and SEO intelligence tool. Enter any URL and it crawls every page, detects technical, content and performance issues, scores each one by severity, and turns everything into a clear, downloadable report. Track your site&apos;s health over time, find broken links, missing meta tags, slow pages and duplicate content, and know exactly what to fix first.
        </p>

        <ul className="hero-rise hero-rise-5 mt-6 flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:gap-8">
          {features.map((feature) => {
            const Icon = feature.icon
            return (
              <li key={feature.label} className="flex items-center gap-2 font-mono text-sm text-main">
                <Icon className="h-4 w-4 text-accent" aria-hidden />
                {feature.label}
              </li>
            )
          })}
        </ul>

        <div className="hero-rise hero-rise-6 mt-7">
          <QuickCheck />
          <button
            type="button"
            onClick={() => smoothScrollTo('solutions')}
            className="mt-4 font-mono text-sm text-main/80 underline underline-offset-4 transition hover:text-accent"
          >
            Learn more about SEOPulse
          </button>
        </div>

        <ul className="hero-rise hero-rise-7 mt-8 flex flex-wrap gap-x-8 gap-y-2">
          {stats.map((stat) => (
            <li key={stat} className="font-mono text-xs tracking-wide text-muted">
              {stat}
            </li>
          ))}
        </ul>
      </div>
    </section>
  )
}
