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
      className={cn('overflow-hidden rounded-2xl border border-default bg-surface card-shadow', className)}
      {...props}
    >
      {(title || description || action) && (
        <div className="flex items-start justify-between gap-3 px-5 pt-5 pb-3 sm:px-6">
          <div className="min-w-0">
            {title && <h2 className="text-headline text-main">{title}</h2>}
            {description && <p className="mt-0.5 text-[13px] text-muted">{description}</p>}
          </div>
          {action && <div className="shrink-0">{action}</div>}
        </div>
      )}
      <div className={padded ? 'p-5 sm:p-6' : undefined}>{children}</div>
    </section>
  )
}
