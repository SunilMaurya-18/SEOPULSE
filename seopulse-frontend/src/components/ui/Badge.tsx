import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'

type BadgeVariant =
  | 'neutral'
  | 'accent'
  | 'success'
  | 'warning'
  | 'critical'
  | 'info'

interface BadgeProps {
  children: ReactNode
  variant?: BadgeVariant
  className?: string
}

const variants: Record<BadgeVariant, string> = {
  neutral: 'border-default bg-surface-elevated text-muted',
  accent: 'border-accent/40 bg-accent-surface text-accent',
  success: 'border-success/30 bg-success-surface text-success',
  warning: 'border-warning/30 bg-warning-surface text-warning',
  critical: 'border-critical/30 bg-critical-surface text-critical',
  info: 'border-info/30 bg-info-surface text-info',
}

export function Badge({
  children,
  variant = 'neutral',
  className,
}: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded border px-1.5 py-0.5',
        'font-mono text-[10px] font-medium tracking-wide uppercase',
        variants[variant],
        className,
      )}
    >
      {children}
    </span>
  )
}
