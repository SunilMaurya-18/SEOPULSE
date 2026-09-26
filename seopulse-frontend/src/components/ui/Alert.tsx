import type { ReactNode } from 'react'
import {
  AlertCircle,
  AlertTriangle,
  CheckCircle2,
  Info,
  X,
} from 'lucide-react'
import { cn } from '@/lib/cn'

type AlertVariant = 'info' | 'success' | 'warning' | 'error'

interface AlertProps {
  variant?: AlertVariant
  title?: string
  children: ReactNode
  onDismiss?: () => void
  className?: string
  action?: ReactNode
}

const styles: Record<AlertVariant, string> = {
  info: 'border-info/30 bg-info-surface text-main',
  success: 'border-success/30 bg-success-surface text-main',
  warning: 'border-warning/30 bg-warning-surface text-main',
  error: 'border-critical/30 bg-critical-surface text-main',
}

const icons = {
  info: Info,
  success: CheckCircle2,
  warning: AlertTriangle,
  error: AlertCircle,
}

const iconColor = {
  info: 'text-info',
  success: 'text-success',
  warning: 'text-warning',
  error: 'text-critical',
}

export function Alert({
  variant = 'info',
  title,
  children,
  onDismiss,
  className,
  action,
}: AlertProps) {
  const Icon = icons[variant]

  return (
    <div
      role="alert"
      className={cn(
        'flex gap-3 rounded-lg border px-4 py-3',
        styles[variant],
        className,
      )}
    >
      <Icon className={cn('mt-0.5 h-4 w-4 shrink-0', iconColor[variant])} />
      <div className="min-w-0 flex-1">
        {title && (
          <p className="text-sm font-semibold text-main">{title}</p>
        )}
        <div className={cn('text-sm text-muted', title && 'mt-0.5')}>
          {children}
        </div>
        {action && <div className="mt-2">{action}</div>}
      </div>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          className="rounded p-1 text-dim hover:bg-surface/60 hover:text-main"
          aria-label="Dismiss"
        >
          <X className="h-4 w-4" />
        </button>
      )}
    </div>
  )
}
