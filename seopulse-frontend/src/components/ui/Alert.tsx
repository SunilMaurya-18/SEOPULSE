import type { ReactNode } from 'react'
import { AlertCircle, AlertTriangle, CheckCircle2, Info, X } from 'lucide-react'
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
  info: 'bg-info-surface',
  success: 'bg-success-surface',
  warning: 'bg-warning-surface',
  error: 'bg-critical-surface',
}

const icons = {
  info: Info,
  success: CheckCircle2,
  warning: AlertTriangle,
  error: AlertCircle,
}

const iconColor = {
  info: 'bg-info',
  success: 'bg-success',
  warning: 'bg-warning',
  error: 'bg-critical',
}

export function Alert({ variant = 'info', title, children, onDismiss, className, action }: AlertProps) {
  const Icon = icons[variant]

  return (
    <div role="alert" className={cn('flex gap-3 rounded-2xl px-4 py-3.5', styles[variant], className)}>
      <span
        className={cn(
          'mt-px flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-white',
          iconColor[variant],
        )}
      >
        <Icon className="h-3.5 w-3.5" strokeWidth={2.5} />
      </span>
      <div className="min-w-0 flex-1">
        {title && <p className="text-sm font-semibold text-main">{title}</p>}
        <div className={cn('text-sm text-muted', title && 'mt-0.5')}>{children}</div>
        {action && <div className="mt-2.5">{action}</div>}
      </div>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          className="flex h-6 w-6 items-center justify-center rounded-full text-dim hover:bg-surface/60 hover:text-main"
          aria-label="Dismiss"
        >
          <X className="h-3.5 w-3.5" />
        </button>
      )}
    </div>
  )
}
