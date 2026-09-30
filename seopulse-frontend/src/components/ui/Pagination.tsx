import { ChevronLeft, ChevronRight } from 'lucide-react'
import { cn } from '@/lib/cn'

interface PaginationProps {
  page: number
  totalPages: number
  totalElements?: number
  onPageChange: (page: number) => void
  className?: string
}

export function Pagination({
  page,
  totalPages,
  totalElements,
  onPageChange,
  className,
}: PaginationProps) {
  if (totalPages <= 1) return null

  const current = page + 1

  return (
    <div
      className={cn(
        'flex items-center justify-between gap-3 border-t border-default px-6 py-3.5',
        className,
      )}
    >
      <p className="text-xs text-muted">
        Page <span className="font-semibold text-main font-tabular">{current}</span> of{' '}
        <span className="font-tabular">{totalPages}</span>
        {typeof totalElements === 'number' && (
          <span className="text-dim"> · {totalElements.toLocaleString()} total</span>
        )}
      </p>
      <div className="flex items-center gap-1 rounded-full bg-surface-low p-1 dark:bg-surface-elevated/60">
        <button
          type="button"
          disabled={page <= 0}
          onClick={() => onPageChange(page - 1)}
          aria-label="Previous page"
          className="flex h-7 w-7 items-center justify-center rounded-full text-main transition-colors hover:bg-surface disabled:pointer-events-none disabled:opacity-30 dark:hover:bg-surface-high"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>
        <button
          type="button"
          disabled={page >= totalPages - 1}
          onClick={() => onPageChange(page + 1)}
          aria-label="Next page"
          className="flex h-7 w-7 items-center justify-center rounded-full text-main transition-colors hover:bg-surface disabled:pointer-events-none disabled:opacity-30 dark:hover:bg-surface-high"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  )
}
