import { useEffect, useState } from 'react'
import { Sparkles, X } from 'lucide-react'
import { Link } from 'react-router-dom'

import { Button } from '@/components/ui/Button'
import { IconTile } from '@/components/ui/IconTile'

export interface UpgradeDetail {
  detail?: string
  used?: number
  limit?: number
  upgradeTo?: string
  meter?: string
}

export function UpgradeModal() {
  const [detail, setDetail] = useState<UpgradeDetail | null>(null)

  useEffect(() => {
    function onUpgrade(event: Event) {
      const custom = event as CustomEvent<UpgradeDetail>
      setDetail(custom.detail ?? { detail: 'This action needs a higher plan.' })
    }
    window.addEventListener('seopulse-upgrade', onUpgrade)
    return () => window.removeEventListener('seopulse-upgrade', onUpgrade)
  }, [])

  useEffect(() => {
    if (!detail) return
    function onKey(event: KeyboardEvent) {
      if (event.key === 'Escape') setDetail(null)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [detail])

  if (!detail) return null

  const hasMeter = typeof detail.used === 'number' && typeof detail.limit === 'number'
  const pct = hasMeter && detail.limit! > 0 ? Math.min(100, Math.round((detail.used! / detail.limit!) * 100)) : 100

  return (
    <div className="fixed inset-0 z-[80] flex items-end justify-center bg-black/45 p-4 backdrop-blur-md sm:items-center">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="upgrade-title"
        className="animate-page-enter relative w-full max-w-md overflow-hidden rounded-[26px] bg-surface p-7 text-center shadow-overlay dark:bg-surface-elevated"
      >
        <div className="pointer-events-none absolute -top-24 left-1/2 h-48 w-72 -translate-x-1/2 rounded-full bg-accent/25 blur-3xl" />
        <button
          type="button"
          onClick={() => setDetail(null)}
          aria-label="Close"
          className="absolute top-4 right-4 flex h-8 w-8 items-center justify-center rounded-full bg-surface-elevated text-muted transition-colors hover:text-main dark:bg-surface-high"
        >
          <X className="h-4 w-4" />
        </button>
        <div className="relative">
          <IconTile tint="coral" size="xl" className="mx-auto">
            <Sparkles />
          </IconTile>
          <h2 id="upgrade-title" className="text-large-title mt-5 text-[28px] text-main">
            Upgrade to keep going
          </h2>
          <p className="mx-auto mt-2 max-w-sm text-[15px] leading-relaxed text-muted">
            {detail.detail || 'This workspace has used its allowance for the current plan.'}
          </p>
          {hasMeter && (
            <div className="mt-5 rounded-2xl bg-surface-low p-4 text-left dark:bg-surface-high/60">
              <div className="flex items-center justify-between text-xs">
                <span className="font-medium text-muted capitalize">{detail.meter ?? 'Usage'}</span>
                <span className="font-semibold text-main font-tabular">
                  {detail.used} / {detail.limit}
                </span>
              </div>
              <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-surface-elevated">
                <div className="h-full rounded-full bg-critical" style={{ width: `${pct}%` }} />
              </div>
            </div>
          )}
          <div className="mt-7 flex flex-col gap-2">
            <Link to="/pricing" onClick={() => setDetail(null)}>
              <Button size="lg" className="w-full">
                {detail.upgradeTo
                  ? `Upgrade to ${detail.upgradeTo.charAt(0).toUpperCase()}${detail.upgradeTo.slice(1).toLowerCase()}`
                  : 'View plans'}
              </Button>
            </Link>
            <Button variant="ghost" size="lg" className="w-full" onClick={() => setDetail(null)}>
              Not now
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}
