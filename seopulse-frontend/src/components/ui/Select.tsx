import type { SelectHTMLAttributes } from 'react'
import { cn } from '@/lib/cn'

interface SelectOption {
  value: string
  label: string
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string
  hint?: string
  error?: string
  options: SelectOption[]
  placeholder?: string
}

export function Select({ label, hint, error, id, options, placeholder, className, ...props }: SelectProps) {
  return (
    <div className="space-y-1.5">
      {label && (
        <label htmlFor={id} className="block text-[13px] font-medium text-main">
          {label}
        </label>
      )}
      <select
        id={id}
        className={cn(
          'h-10 w-full rounded-[var(--sp-field-radius,0.25rem)] border bg-[var(--sp-field-bg,var(--sp-surface))] px-3.5 text-sm text-main',
          'outline-none transition focus:border-accent/60 focus:ring-4 focus:ring-accent/15',
          error ? 'border-critical' : 'border-default',
          className,
        )}
        aria-invalid={Boolean(error)}
        {...props}
      >
        {placeholder && (
          <option value="" disabled>
            {placeholder}
          </option>
        )}
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
      {error ? (
        <p className="text-xs font-medium text-critical">{error}</p>
      ) : hint ? (
        <p className="text-xs text-muted">{hint}</p>
      ) : null}
    </div>
  )
}
