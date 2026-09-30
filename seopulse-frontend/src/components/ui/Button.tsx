import type { ButtonHTMLAttributes } from 'react'
import { Loader2 } from 'lucide-react'
import { cn } from '@/lib/cn'

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger'
type ButtonSize = 'sm' | 'md' | 'lg'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
  loading?: boolean
}

const variants: Record<ButtonVariant, string> = {
  primary:
    'bg-accent text-on-accent border border-accent shadow-[0_6px_16px_-8px_var(--sp-accent)] hover:bg-accent-hover hover:border-accent-hover focus-visible:ring-accent',
  secondary:
    'border border-default bg-surface text-main hover:bg-surface-elevated focus-visible:ring-focus',
  ghost:
    'border border-transparent text-muted hover:bg-surface-elevated hover:text-main focus-visible:ring-focus',
  danger:
    'border border-critical/40 bg-critical-surface text-critical hover:bg-critical hover:text-white focus-visible:ring-critical',
}

const sizes: Record<ButtonSize, string> = {
  sm: 'h-8 px-3.5 text-[13px]',
  md: 'h-9 px-4 text-sm',
  lg: 'h-11 px-5 text-[15px]',
}

export function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled,
  children,
  className,
  ...props
}: ButtonProps) {
  return (
    <button
      disabled={disabled || loading}
      className={cn(
        'inline-flex items-center justify-center gap-1.5 rounded-[var(--sp-btn-radius,0.375rem)] font-mono font-semibold whitespace-nowrap transition-all active:scale-[0.97]',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-offset-canvas',
        'disabled:pointer-events-none disabled:opacity-45',
        variants[variant],
        sizes[size],
        className,
      )}
      {...props}
    >
      {loading && <Loader2 className="h-4 w-4 animate-spin" aria-hidden />}
      {children}
    </button>
  )
}
