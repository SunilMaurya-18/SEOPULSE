import type { ReactNode } from 'react'
import { ArrowRight } from 'lucide-react'
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
    <div
      className={cn(
        'relative overflow-hidden rounded-xl border border-accent/25 bg-[linear-gradient(120deg,var(--sp-accent-surface),transparent_55%)] px-4 py-4 sm:px-5',
        className,
      )}
    >
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex min-w-0 items-start gap-3">
          {icon && (
            <div className="mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border border-accent/20 bg-surface text-accent">
              {icon}
            </div>
          )}
          <div className="min-w-0">
            <p className="font-mono text-[10px] tracking-[0.14em] text-accent uppercase">
              {eyebrow}
            </p>
            <p className="mt-1 font-display text-base font-semibold text-main">
              {title}
            </p>
            <p className="mt-1 text-sm leading-6 text-muted">{description}</p>
          </div>
        </div>
        <div className="shrink-0">
          {to ? (
            <Link
              to={to}
              onClick={onAction}
              className="inline-flex h-9 items-center justify-center gap-2 rounded-lg border border-accent bg-accent px-3.5 text-sm font-medium text-white transition hover:bg-accent-hover"
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
