import { useMemo, useState } from 'react'
import { Search, Shield } from 'lucide-react'

import { TOC_ITEMS, type TocItem } from './data'

type DocumentNavProps = {
  activeId: string
  onNavigate: (id: string) => void
}

function badgeClass(item: TocItem) {
  if (item.badgeTone === 'strict') return 'font-semibold text-[#dc2626]'
  if (item.badgeTone === 'sla') return 'font-semibold text-emerald-400'
  if (item.badgeTone === 'binding') return 'text-zinc-400'
  return 'text-zinc-500'
}

export function DocumentNav({ activeId, onNavigate }: DocumentNavProps) {
  const [query, setQuery] = useState('')

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return TOC_ITEMS
    return TOC_ITEMS.filter(
      (item) =>
        item.title.toLowerCase().includes(q) ||
        item.section.toLowerCase().includes(q) ||
        item.badge.toLowerCase().includes(q),
    )
  }, [query])

  return (
    <aside className="sticky top-20 flex flex-col gap-4 lg:col-span-4">
      <div className="space-y-2 rounded-lg border border-[#27272a] bg-[#131315] p-4 shadow-sm">
        <div className="flex items-center justify-between">
          <span className="font-mono text-[11px] font-semibold tracking-wider text-zinc-400 uppercase">
            Document Navigator
          </span>
          <span className="font-mono text-[11px] text-zinc-500">11 CLAUSES</span>
        </div>
        <div className="relative">
          <Search className="absolute top-2.5 left-3 h-[18px] w-[18px] text-zinc-500" />
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search clause (e.g. SLA, API, Liability)..."
            className="h-9 w-full rounded border border-[#27272a] bg-[#0e0e10] pr-3 pl-9 text-xs text-white placeholder-zinc-500 transition-colors focus:border-[#b91c1c] focus:outline-none"
          />
        </div>
      </div>

      <nav className="flex max-h-[593px] flex-col space-y-1 overflow-y-auto rounded-lg border border-[#27272a] bg-[#131315] p-4 shadow-sm">
        {filtered.map((item) => {
          const active = activeId === item.id
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => onNavigate(item.id)}
              className={
                active
                  ? 'group flex items-center justify-between rounded border border-[#b91c1c]/40 bg-[#1c1b1d] p-2 text-left font-semibold text-white transition-colors'
                  : 'group flex items-center justify-between rounded border border-transparent p-2 text-left text-zinc-300 transition-colors hover:border-[#27272a] hover:bg-[#18181b] hover:text-white'
              }
            >
              <span className="truncate text-xs">
                <strong className="mr-1.5 font-mono text-[#dc2626]">
                  {item.section}
                </strong>
                {item.title}
              </span>
              <span
                className={`font-mono text-[10px] ${badgeClass(item)} ${active ? '' : ''}`}
              >
                {item.badge}
              </span>
            </button>
          )
        })}
        {filtered.length === 0 && (
          <p className="px-2 py-3 font-mono text-[11px] text-zinc-500">
            No clauses match your search.
          </p>
        )}
      </nav>

      <div className="space-y-1 rounded-lg border border-[#27272a] bg-[#101012] p-4 shadow-sm">
        <div className="flex items-center gap-2 text-lg font-semibold text-[#dc2626]">
          <Shield className="h-5 w-5" />
          <span className="text-white">Counsel Registry</span>
        </div>
        <p className="text-[13px] leading-[22px] text-white">
          Direct formal inquiries, subpoena issuances, and trademark challenges
          to our office of technical jurisprudence.
        </p>
        <div className="space-y-1 pt-2 font-mono text-[11px] text-zinc-400">
          <div>
            EMAIL:{' '}
            <a
              className="text-white underline transition-colors hover:text-[#dc2626]"
              href="mailto:legal@seopulse.io"
            >
              legal@seopulse.io
            </a>
          </div>
          <div className="truncate text-zinc-400">
            PGP: 4A7B 8932 EF01 C812 5590
          </div>
          <div className="font-medium text-emerald-400">
            STATUS: Continuous Monitoring
          </div>
        </div>
      </div>
    </aside>
  )
}
