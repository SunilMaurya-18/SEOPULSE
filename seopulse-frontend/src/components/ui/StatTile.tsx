import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { ChevronRight } from 'lucide-react'

import { IconTile } from './IconTile'
import type { Tint } from './tints'
import { cn } from '@/lib/cn'

interface StatTileProps {
  tint: Tint
  icon: ReactNode
  label: string
  value: ReactNode
  caption?: ReactNode
  valueClassName?: string
  to?: string
  className?: string
}

export function StatTile({ tint, icon, label, value, caption, valueClassName, to, className }: StatTileProps) {
  const body = (
    <>
      <div className="flex items-center gap-2.5">
        <IconTile tint={tint}>{icon}</IconTile>
        <p className="min-w-0 flex-1 truncate text-[13px] font-semibold text-muted">{label}</p>
        {to && (
          <ChevronRight className="h-4 w-4 text-dim transition-transform group-hover:translate-x-0.5 group-hover:text-main" />
        )}
      </div>
      <p className={cn('num mt-4 text-[30px] leading-none font-bold text-main', valueClassName)}>{value}</p>
      {caption && <p className="mt-2 truncate text-xs text-dim">{caption}</p>}
    </>
  )

  const classes = cn('widget group block p-5 transition-transform duration-300 hover:-translate-y-0.5', className)

  return to ? (
    <Link to={to} className={classes}>
      {body}
    </Link>
  ) : (
    <div className={classes}>{body}</div>
  )
}
