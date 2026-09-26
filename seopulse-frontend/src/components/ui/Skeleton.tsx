import { cn } from '@/lib/cn'

interface SkeletonProps {
  className?: string
}

export function Skeleton({ className }: SkeletonProps) {
  return (
    <div
      className={cn(
        'animate-pulse rounded bg-surface-elevated',
        className,
      )}
      aria-hidden
    />
  )
}

export function TableSkeleton({ rows = 5 }: { rows?: number }) {
  return (
    <div className="space-y-0 divide-y divide-default/60" role="status" aria-label="Loading">
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="flex items-center gap-4 px-5 py-4">
          <Skeleton className="h-4 w-1/3" />
          <Skeleton className="h-4 w-16" />
          <Skeleton className="ml-auto h-4 w-20" />
        </div>
      ))}
    </div>
  )
}

export function CardSkeleton() {
  return (
    <div className="rounded-lg border border-default bg-surface p-5" role="status">
      <Skeleton className="h-3 w-24" />
      <Skeleton className="mt-4 h-8 w-16" />
      <Skeleton className="mt-2 h-3 w-32" />
    </div>
  )
}

export function PageSkeleton() {
  return (
    <div className="space-y-6" role="status" aria-label="Loading page">
      <div>
        <Skeleton className="h-3 w-20" />
        <Skeleton className="mt-2 h-8 w-48" />
        <Skeleton className="mt-2 h-4 w-72 max-w-full" />
      </div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <CardSkeleton />
        <CardSkeleton />
        <CardSkeleton />
        <CardSkeleton />
      </div>
      <div className="rounded-lg border border-default bg-surface">
        <div className="border-b border-default px-5 py-4">
          <Skeleton className="h-4 w-32" />
        </div>
        <TableSkeleton />
      </div>
    </div>
  )
}
