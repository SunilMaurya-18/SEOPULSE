import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Activity } from 'lucide-react'

export function AuthShell({
  title,
  description,
  children,
  footer,
}: {
  title: string
  description?: string
  children: ReactNode
  footer?: ReactNode
}) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-canvas px-4">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center">
          <Link to="/" className="inline-flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded bg-accent text-white">
              <Activity className="h-4 w-4" strokeWidth={2.25} />
            </div>
            <span className="font-display text-lg font-semibold text-main">
              SEOPulse
            </span>
          </Link>
          <h1 className="mt-6 font-display text-2xl font-semibold text-main">
            {title}
          </h1>
          {description && <p className="mt-1 text-sm text-muted">{description}</p>}
        </div>

        <div className="space-y-4 rounded-lg border border-default bg-surface p-5">
          {children}
        </div>

        {footer && <p className="text-center text-sm text-muted">{footer}</p>}
      </div>
    </div>
  )
}
