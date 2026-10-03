import { Activity, FileSearch, FileWarning, Link2Off, Mail, Type, Zap } from 'lucide-react'

import { Logo } from '@/components/brand/Logo'
import { IconTile } from '@/components/ui/IconTile'
import { ScoreRing } from '@/components/ui/ScoreRing'
import type { Tint } from '@/components/ui/tints'
import { cn } from '@/lib/cn'

const STATS: { label: string; value: string; tint: Tint; icon: typeof Activity }[] = [
  { label: 'Pages crawled', value: '1,248', tint: 'blue', icon: FileSearch },
  { label: 'Issues fixed', value: '86', tint: 'green', icon: Activity },
  { label: 'Avg. response', value: '0.4s', tint: 'purple', icon: Zap },
]

const ISSUES: { label: string; pages: number; tint: Tint; icon: typeof Activity }[] = [
  { label: 'Broken internal links', pages: 4, tint: 'red', icon: Link2Off },
  { label: 'Missing meta descriptions', pages: 12, tint: 'orange', icon: FileWarning },
  { label: 'Duplicate page titles', pages: 7, tint: 'yellow', icon: Type },
]

const FEATURES: { title: string; body: string; tint: Tint; icon: typeof Activity }[] = [
  { title: 'Live crawls', body: 'Watch every page get scored', tint: 'coral', icon: Activity },
  { title: 'Ranked issues', body: 'Fix what moves rankings first', tint: 'orange', icon: FileWarning },
  { title: 'Emailed reports', body: 'Share results in one click', tint: 'blue', icon: Mail },
]

export function AuthShowcase() {
  return (
    <aside
      aria-hidden="true"
      className="relative m-3 hidden w-[52%] max-w-[760px] shrink-0 flex-col overflow-hidden rounded-[32px] bg-surface-low p-10 card-shadow lg:flex [@media(max-height:880px)]:p-8"
    >
      <div className="pointer-events-none absolute -top-40 -left-24 h-[420px] w-[420px] rounded-full bg-white/10 blur-[110px]" />
      <div className="pointer-events-none absolute -right-24 bottom-10 h-[360px] w-[360px] rounded-full bg-white/5 blur-[110px]" />

      <div className="relative">
        <Logo to="/" appearance="app" />
      </div>

      <div className="relative mt-8 max-w-[30rem]">
        <p className="text-[13px] font-semibold text-accent">SEO audits, reimagined</p>
        <h2 className="text-glow mt-3 font-display text-[36px] leading-[1.05] font-bold tracking-[-0.035em] text-main xl:text-[44px]">
          Every signal your site sends.
          <span className="block bg-gradient-to-br from-main to-main/50 bg-clip-text text-transparent">
            One calm dashboard.
          </span>
        </h2>
        <p className="mt-4 text-[15px] leading-relaxed text-muted [@media(max-height:880px)]:hidden">
          Crawl your websites, score on-page health and get a ranked list of fixes, all in one quiet
          workspace.
        </p>
      </div>

      <div className="relative mt-8 flex flex-1 flex-col justify-center">
        <div className="widget w-[78%] p-6 pb-14">
          <div className="flex items-center gap-6">
            <ScoreRing score={92} size={128} strokeWidth={11} label="health" />
            <div className="min-w-0 flex-1 space-y-3">
              <div>
                <p className="text-headline text-main">example.com</p>
                <p className="text-[12px] text-dim">Audited 2 minutes ago</p>
              </div>
              {STATS.map(({ label, value, tint, icon: Icon }) => (
                <div key={label} className="flex items-center gap-2.5">
                  <IconTile tint={tint} size="sm">
                    <Icon strokeWidth={2.25} />
                  </IconTile>
                  <span className="flex-1 truncate text-[13px] text-muted">{label}</span>
                  <span className="num text-[15px] font-semibold text-main">{value}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="widget relative -mt-10 ml-auto w-[62%] p-5 shadow-[var(--sp-overlay-shadow)]">
          <div className="mb-3 flex items-center justify-between">
            <p className="text-[13px] font-semibold text-main">Top issues</p>
            <span className="rounded-full bg-accent-surface px-2 py-0.5 text-[11px] font-semibold text-accent">
              23 open
            </span>
          </div>
          <div className="space-y-2.5">
            {ISSUES.map(({ label, pages, tint, icon: Icon }, index) => (
              <div
                key={label}
                className={cn(
                  'flex items-center gap-3 rounded-2xl bg-surface-elevated/60 px-3 py-2.5',
                  index === ISSUES.length - 1 && '[@media(max-height:880px)]:hidden',
                )}
              >
                <IconTile tint={tint} size="sm">
                  <Icon strokeWidth={2.25} />
                </IconTile>
                <span className="flex-1 truncate text-[13px] font-medium text-main">{label}</span>
                <span className="text-[12px] text-dim">
                  {pages} {pages === 1 ? 'page' : 'pages'}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="relative mt-10 grid grid-cols-3 gap-4 [@media(max-height:980px)]:hidden">
        {FEATURES.map(({ title, body, tint, icon: Icon }) => (
          <div key={title} className="flex items-start gap-3">
            <IconTile tint={tint}>
              <Icon strokeWidth={2.25} />
            </IconTile>
            <div className="min-w-0">
              <p className="text-[13px] font-semibold text-main">{title}</p>
              <p className="text-[12px] leading-snug text-dim">{body}</p>
            </div>
          </div>
        ))}
      </div>
    </aside>
  )
}
