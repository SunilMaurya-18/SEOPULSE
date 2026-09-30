import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'

type BadgeVariant = 'neutral' | 'accent' | 'success' | 'warning' | 'critical' | 'info'

interface BadgeProps {
  children: ReactNode
  variant?: BadgeVariant
  className?: string
}

const variants: Record<BadgeVariant, string> = {
  neutral: 'bg-surface-elevated text-muted',
  accent: 'bg-accent-surface text-accent',
  success: 'bg-success-surface text-success',
  warning: 'bg-warning-surface text-warning',
  critical: 'bg-critical-surface text-critical',
  info: 'bg-info-surface text-info',
}

export function Badge({ children, variant = 'neutral', className }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-semibold whitespace-nowrap',
        variants[variant],
        className,
      )}
    >
      {children}
    </span>
  )
}
