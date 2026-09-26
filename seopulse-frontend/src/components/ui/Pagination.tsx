import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from './Button'
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
        'flex flex-col gap-3 border-t border-default px-4 py-3 sm:flex-row sm:items-center sm:justify-between sm:px-5',
        className,
      )}
    >
      <p className="font-mono text-[11px] text-muted">
        Page {current} of {totalPages}
        {typeof totalElements === 'number' && (
          <span className="text-dim"> · {totalElements} total</span>
        )}
      </p>
      <div className="flex items-center gap-2">
        <Button
          variant="secondary"
          size="sm"
          disabled={page <= 0}
          onClick={() => onPageChange(page - 1)}
          aria-label="Previous page"
        >
          <ChevronLeft className="h-4 w-4" />
          Prev
        </Button>
        <Button
          variant="secondary"
          size="sm"
          disabled={page >= totalPages - 1}
          onClick={() => onPageChange(page + 1)}
          aria-label="Next page"
        >
          Next
          <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  )
}
