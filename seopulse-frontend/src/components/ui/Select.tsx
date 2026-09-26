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

export function Select({
  label,
  hint,
  error,
  id,
  options,
  placeholder,
  className,
  ...props
}: SelectProps) {
  return (
    <div className="space-y-1.5">
      {label && (
        <label htmlFor={id} className="block text-sm font-medium text-main">
          {label}
        </label>
      )}
      <select
        id={id}
        className={cn(
          'h-9 w-full rounded border bg-surface px-3 text-sm text-main',
          'outline-none transition focus:border-focus focus:ring-1 focus:ring-accent/30',
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
        <p className="font-mono text-[11px] text-critical">{error}</p>
      ) : hint ? (
        <p className="text-xs text-muted">{hint}</p>
      ) : null}
    </div>
  )
}
