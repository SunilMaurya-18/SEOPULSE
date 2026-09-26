import { cn } from '@/lib/cn'

export interface TabItem {
  id: string
  label: string
  count?: number
}

interface TabsProps {
  items: TabItem[]
  value: string
  onChange: (id: string) => void
  className?: string
}

export function Tabs({ items, value, onChange, className }: TabsProps) {
  return (
    <div
      role="tablist"
      className={cn(
        'flex flex-wrap gap-1 border-b border-default',
        className,
      )}
    >
      {items.map((item) => {
        const active = item.id === value
        return (
          <button
            key={item.id}
            type="button"
            role="tab"
            aria-selected={active}
            onClick={() => onChange(item.id)}
            className={cn(
              '-mb-px inline-flex items-center gap-2 border-b-2 px-3 py-2.5 text-sm transition-colors',
              active
                ? 'border-accent font-medium text-main'
                : 'border-transparent text-muted hover:text-main',
            )}
          >
            {item.label}
            {typeof item.count === 'number' && (
              <span
                className={cn(
                  'rounded border px-1.5 py-0 font-mono text-[10px]',
                  active
                    ? 'border-accent/30 bg-accent-surface text-accent'
                    : 'border-default bg-surface-elevated text-dim',
                )}
              >
                {item.count}
              </span>
            )}
          </button>
        )
      })}
    </div>
  )
}
