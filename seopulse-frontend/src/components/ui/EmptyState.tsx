import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'

interface EmptyStateProps {
  icon?: ReactNode
  title: string
  description?: string
  action?: ReactNode
  className?: string
}

export function EmptyState({ icon, title, description, action, className }: EmptyStateProps) {
  return (
    <div className={cn('flex min-h-52 items-center justify-center px-6 py-12', className)}>
      <div className="max-w-sm text-center">
        {icon && (
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-[18px] bg-gradient-to-b from-surface-elevated to-surface-high text-muted shadow-[inset_0_1px_0_rgb(255_255_255/0.06)]">
            {icon}
          </div>
        )}
        <h3 className="text-headline mt-5 text-main">{title}</h3>
        {description && <p className="mt-1.5 text-sm leading-relaxed text-muted">{description}</p>}
        {action && <div className="mt-5 flex justify-center">{action}</div>}
      </div>
    </div>
  )
}
