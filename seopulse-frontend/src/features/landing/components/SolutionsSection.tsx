import { ChartColumn, CloudDownload, Cpu, Network, type LucideIcon } from 'lucide-react'
import { cn } from '@/lib/cn'
import { useInView } from '@/features/landing/useInView'

const cards: Array<{
  title: string
  body: string
  icon: LucideIcon
  rotate: string
  drift: string
  offset: string
}> = [
  {
    title: 'Technical Site Crawls',
    body: 'A bounded crawl that follows sitemaps and stays inside the host you named.',
    icon: CloudDownload,
    rotate: '-rotate-2',
    drift: 'parallax-slow',
    offset: 'sm:mt-0',
  },
  {
    title: 'Issue Analysis',
    body: 'Titles, headings, links, images, and status codes scored on every page fetched.',
    icon: ChartColumn,
    rotate: 'rotate-2',
    drift: 'parallax-fast',
    offset: 'sm:mt-16',
  },
  {
    title: 'Page Inventory',
    body: 'Each URL kept with its depth, status, and outcome so nothing in the run is anonymous.',
    icon: Cpu,
    rotate: 'rotate-1',
    drift: 'parallax-mid',
    offset: 'sm:-mt-6',
  },
  {
    title: 'Reports You Can Share',
    body: 'A finished audit downloads as a report the next person can read without an account.',
    icon: Network,
    rotate: '-rotate-1',
    drift: 'parallax-slow',
    offset: 'sm:mt-10',
  },
]

export function SolutionsSection() {
  const { ref, shown } = useInView<HTMLDivElement>()

  return (
    <section
      id="solutions"
      className="scroll-mt-24 bg-[linear-gradient(180deg,#243640_0%,#05070a_26%)] px-5 py-24 md:px-[60px] md:py-32"
    >
      <div className="mx-auto grid max-w-6xl items-start gap-14 lg:grid-cols-2 lg:gap-20">
        <div ref={ref} className="lg:sticky lg:top-28">
          <h2
            className={cn(
              'font-mono text-4xl leading-tight font-normal tracking-wide text-[#f2f5ea] transition duration-700 sm:text-5xl lg:text-6xl',
              shown ? 'translate-y-0 opacity-100 blur-none' : 'translate-y-4 opacity-50 blur-md',
            )}
          >
            Let your data take your rankings to higher ground.
          </h2>
          <div className="mt-8 flex gap-4">
            <span className="w-px shrink-0 bg-accent" aria-hidden />
            <p className="font-mono text-sm leading-relaxed text-[#f2f5ea] sm:text-base">
              SEOPulse crawls the public pages you point it at, scores what it finds,
              and keeps the crawl, the issues, and the report in one place.
            </p>
          </div>
        </div>

        <div className="grid gap-5 sm:grid-cols-2">
          {cards.map((card) => {
            const Icon = card.icon
            return (
              <div key={card.title} className={cn(card.drift, card.offset)}>
                <article
                  className={cn(
                    'rounded-lg bg-[#4a4d47] p-5 text-[#f2f5ea] transition duration-300 hover:rotate-0 max-sm:rotate-0',
                    card.rotate,
                  )}
                >
                  <div className="flex h-11 w-11 items-center justify-center rounded-md border border-white/80">
                    <Icon className="h-5 w-5" strokeWidth={1.5} aria-hidden />
                  </div>
                  <h3 className="mt-5 font-mono text-base font-medium">{card.title}</h3>
                  <p className="mt-2 font-mono text-sm leading-relaxed text-[#f2f5ea]/85">
                    {card.body}
                  </p>
                </article>
              </div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
