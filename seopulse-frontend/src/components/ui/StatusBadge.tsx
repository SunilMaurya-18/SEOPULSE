import { AlertTriangle, Ban, CornerDownRight, OctagonAlert } from 'lucide-react'
import { Badge } from './Badge'

type Status =
  | 'QUEUED'
  | 'CRAWLING'
  | 'CRAWLED'
  | 'ANALYZING'
  | 'COMPLETED'
  | 'REDIRECT'
  | 'SKIPPED_ROBOTS'
  | 'TOO_LARGE'
  | 'FAILED'
  | 'CANCELLED'
  | 'ACTIVE'
  | 'LIVE'
  | string

export function StatusBadge({ status }: { status: Status }) {
  const normalized = status.toUpperCase()

  if (normalized === 'REDIRECT') {
    return (
      <Badge variant="info">
        <CornerDownRight className="h-3 w-3" aria-hidden />
        Redirect
      </Badge>
    )
  }

  if (normalized === 'SKIPPED_ROBOTS') {
    return (
      <Badge variant="neutral">
        <Ban className="h-3 w-3" aria-hidden />
        Blocked by robots.txt
      </Badge>
    )
  }

  if (
    normalized === 'COMPLETED' ||
    normalized === 'CRAWLED' ||
    normalized === 'ACTIVE' ||
    normalized === 'LIVE'
  ) {
    return (
      <Badge variant="success">
        <span className="h-1.5 w-1.5 rounded-full bg-success" aria-hidden />
        {formatStatus(normalized)}
      </Badge>
    )
  }

  if (normalized === 'FAILED') {
    return (
      <Badge variant="critical">
        <OctagonAlert className="h-3 w-3" aria-hidden />
        Failed
      </Badge>
    )
  }

  if (normalized === 'CANCELLED') {
    return (
      <Badge variant="neutral">
        <Ban className="h-3 w-3" aria-hidden />
        Cancelled
      </Badge>
    )
  }

  if (normalized === 'CRAWLING' || normalized === 'ANALYZING') {
    return (
      <Badge variant="info">
        <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-info" aria-hidden />
        {formatStatus(normalized)}
      </Badge>
    )
  }

  return (
    <Badge variant="warning">
      <AlertTriangle className="h-3 w-3" aria-hidden />
      {formatStatus(normalized)}
    </Badge>
  )
}

function formatStatus(status: string) {
  return status
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
}
