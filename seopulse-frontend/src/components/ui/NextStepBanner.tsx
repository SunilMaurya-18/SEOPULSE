import type { ReactNode } from 'react'
import { ArrowRight, Sparkles } from 'lucide-react'
import { Link } from 'react-router-dom'
import { cn } from '@/lib/cn'
import { Button } from '@/components/ui/Button'

interface NextStepBannerProps {
  eyebrow?: string
  title: string
  description: string
  actionLabel: string
  to?: string
  icon?: ReactNode
  className?: string
  onAction?: () => void
}

export function NextStepBanner({
  eyebrow = 'Next step',
  title,
  description,
  actionLabel,
  to,
  icon,
  className,
  onAction,
}: NextStepBannerProps) {
  return (
    <div className={cn('widget relative overflow-hidden px-5 py-5 sm:px-6', className)}>
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(120%_140%_at_0%_0%,var(--sp-accent-surface),transparent_55%)]" />
      <div className="relative flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex min-w-0 items-start gap-4">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-[14px] bg-gradient-to-b from-[#ff6b5f] to-[#e8413b] text-white shadow-[inset_0_1px_0_rgb(255_255_255/0.3),0_8px_20px_-8px_rgb(245_80_74/0.7)]">
            {icon ?? <Sparkles className="h-5 w-5" />}
          </div>
          <div className="min-w-0">
            <p className="text-xs font-semibold text-accent">{eyebrow}</p>
            <p className="text-headline mt-0.5 text-main">{title}</p>
            <p className="mt-1 text-sm leading-relaxed text-muted">{description}</p>
          </div>
        </div>
        <div className="shrink-0">
          {to ? (
            <Link
              to={to}
              onClick={onAction}
              className="inline-flex h-9 items-center justify-center gap-1.5 rounded-full bg-accent px-4 text-sm font-semibold text-on-accent shadow-[0_6px_16px_-8px_var(--sp-accent)] transition hover:bg-accent-hover"
            >
              {actionLabel}
              <ArrowRight className="h-4 w-4" />
            </Link>
          ) : (
            <Button onClick={onAction}>
              {actionLabel}
              <ArrowRight className="h-4 w-4" />
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}
