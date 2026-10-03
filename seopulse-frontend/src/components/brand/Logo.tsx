import { SlidersHorizontal } from 'lucide-react'
import { Link } from 'react-router-dom'
import { cn } from '@/lib/cn'

export function Logo({
  to = '/',
  className,
  onClick,
  appearance = 'mono',
}: {
  to?: string
  className?: string
  onClick?: () => void
  appearance?: 'mono' | 'app'
}) {
  if (appearance === 'app') {
    return (
      <Link to={to} onClick={onClick} className={cn('inline-flex items-center gap-2.5', className)}>
        <span className="flex h-8 w-8 items-center justify-center rounded-[9px] bg-accent shadow-[inset_0_1px_0_rgb(255_255_255/0.3)]">
          <SlidersHorizontal className="h-4 w-4 text-on-accent" strokeWidth={2.5} aria-hidden />
        </span>
        <span className="font-display text-[17px] font-semibold tracking-[-0.02em] text-main">SEOPulse</span>
      </Link>
    )
  }
  return (
    <Link to={to} onClick={onClick} className={cn('inline-flex items-center gap-2.5', className)}>
      <SlidersHorizontal className="h-5 w-5 text-accent" strokeWidth={2.25} aria-hidden />
      <span className="font-mono text-sm tracking-[0.18em] text-main">SEOPulse</span>
    </Link>
  )
}
