import type { HTMLAttributes, ReactNode } from 'react'
import { cn } from '@/lib/cn'

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  title?: string
  description?: string
  action?: ReactNode
  padded?: boolean
}

export function Card({
  title,
  description,
  action,
  padded = false,
  children,
  className,
  ...props
}: CardProps) {
  return (
    <section
      className={cn(
        'rounded-xl border border-default bg-surface',
        className,
      )}
      {...props}
    >
      {(title || description || action) && (
        <div className="flex items-start justify-between gap-3 border-b border-default/80 px-4 py-3.5 sm:px-5">
          <div className="min-w-0">
            {title && (
              <h2 className="font-display text-sm font-semibold text-main">
                {title}
              </h2>
            )}
            {description && (
              <p className="mt-0.5 text-xs text-muted">{description}</p>
            )}
          </div>
          {action && <div className="shrink-0">{action}</div>}
        </div>
      )}
      <div className={padded ? 'p-4 sm:p-5' : undefined}>{children}</div>
    </section>
  )
}
