import type { InputHTMLAttributes } from 'react'
import { cn } from '@/lib/cn'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  hint?: string
  error?: string
}

export function Input({
  label,
  hint,
  error,
  id,
  className,
  ...props
}: InputProps) {
  return (
    <div className="space-y-1.5">
      {label && (
        <label htmlFor={id} className="block text-sm font-medium text-main">
          {label}
        </label>
      )}
      <input
        id={id}
        className={cn(
          'h-9 w-full rounded border bg-surface px-3 text-sm text-main placeholder:text-dim',
          'outline-none transition focus:border-focus focus:ring-1 focus:ring-accent/30',
          error ? 'border-critical' : 'border-default',
          className,
        )}
        aria-invalid={Boolean(error)}
        {...props}
      />
      {error ? (
        <p className="font-mono text-[11px] text-critical">{error}</p>
      ) : hint ? (
        <p className="text-xs text-muted">{hint}</p>
      ) : null}
    </div>
  )
}
