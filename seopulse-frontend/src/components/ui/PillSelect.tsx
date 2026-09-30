import type { ReactNode, SelectHTMLAttributes } from 'react'
import { ChevronDown, Search } from 'lucide-react'
import { cn } from '@/lib/cn'

interface PillSelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string
  icon?: ReactNode
  options: Array<{ value: string; label: string }>
  placeholder?: string
}

export function PillSelect({ label, icon, options, placeholder, className, ...props }: PillSelectProps) {
  return (
    <label
      className={cn(
        'relative inline-flex h-10 min-w-0 items-center gap-2 rounded-full bg-surface pr-9 pl-3.5 card-shadow transition focus-within:ring-4 focus-within:ring-accent/15 dark:bg-surface-elevated/70',
        className,
      )}
    >
      {icon && <span className="shrink-0 text-dim [&_svg]:h-4 [&_svg]:w-4">{icon}</span>}
      <span className="shrink-0 text-xs font-medium text-dim">{label}</span>
      <select
        aria-label={label}
        className="min-w-0 flex-1 cursor-pointer appearance-none truncate bg-transparent text-[13px] font-semibold text-main outline-none"
        {...props}
      >
        {placeholder && (
          <option value="" disabled>
            {placeholder}
          </option>
        )}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <ChevronDown className="pointer-events-none absolute right-3.5 h-4 w-4 text-dim" />
    </label>
  )
}

export function SearchField({
  value,
  onChange,
  placeholder,
  label,
  className,
}: {
  value: string
  onChange: (value: string) => void
  placeholder: string
  label: string
  className?: string
}) {
  return (
    <label
      className={cn(
        'relative inline-flex h-10 min-w-0 items-center rounded-full bg-surface card-shadow transition focus-within:ring-4 focus-within:ring-accent/15 dark:bg-surface-elevated/70',
        className,
      )}
    >
      <Search className="pointer-events-none absolute left-3.5 h-4 w-4 text-dim" />
      <input
        type="search"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        aria-label={label}
        className="h-full w-full min-w-0 rounded-full bg-transparent pr-4 pl-10 text-[13px] text-main outline-none placeholder:text-dim"
      />
    </label>
  )
}
