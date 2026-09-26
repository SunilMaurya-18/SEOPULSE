import {
  AlertCircle,
  AlertTriangle,
  CheckCircle2,
  Info,
  OctagonAlert,
} from 'lucide-react'
import { Badge } from './Badge'

type Status =
  | 'QUEUED'
  | 'CRAWLING'
  | 'ANALYZING'
  | 'COMPLETED'
  | 'FAILED'
  | 'ACTIVE'
  | 'LIVE'
  | string

export function StatusBadge({ status }: { status: Status }) {
  const normalized = status.toUpperCase()

  if (normalized === 'COMPLETED' || normalized === 'ACTIVE' || normalized === 'LIVE') {
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

type Severity = 'CRITICAL' | 'ERROR' | 'WARNING' | 'INFO' | 'PASSED' | string

export function SeverityBadge({ severity }: { severity: Severity }) {
  const normalized = severity.toUpperCase()

  if (normalized === 'CRITICAL' || normalized === 'ERROR') {
    return (
      <Badge variant="critical">
        <AlertCircle className="h-3 w-3" aria-hidden />
        {normalized === 'CRITICAL' ? 'Critical' : 'Error'}
      </Badge>
    )
  }

  if (normalized === 'WARNING' || normalized === 'WARN') {
    return (
      <Badge variant="warning">
        <AlertTriangle className="h-3 w-3" aria-hidden />
        Warning
      </Badge>
    )
  }

  if (normalized === 'PASSED' || normalized === 'SUCCESS') {
    return (
      <Badge variant="success">
        <CheckCircle2 className="h-3 w-3" aria-hidden />
        Passed
      </Badge>
    )
  }

  return (
    <Badge variant="info">
      <Info className="h-3 w-3" aria-hidden />
      Info
    </Badge>
  )
}

function formatStatus(status: string) {
  return status
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
}
