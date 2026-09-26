import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'

interface EmptyStateProps {
  icon?: ReactNode
  title: string
  description?: string
  action?: ReactNode
  className?: string
}

export function EmptyState({
  icon,
  title,
  description,
  action,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        'flex min-h-48 items-center justify-center px-6 py-10',
        className,
      )}
    >
      <div className="max-w-sm text-center">
        {icon && (
          <div className="mx-auto flex h-11 w-11 items-center justify-center rounded-lg border border-default bg-surface-low text-muted">
            {icon}
          </div>
        )}
        <h3 className="mt-4 text-sm font-semibold text-main">{title}</h3>
        {description && (
          <p className="mt-1.5 text-sm leading-6 text-muted">{description}</p>
        )}
        {action && <div className="mt-4">{action}</div>}
      </div>
    </div>
  )
}
