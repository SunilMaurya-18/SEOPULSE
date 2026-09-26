import { Activity } from 'lucide-react'
import { Link } from 'react-router-dom'

export function TermsHeader() {
  return (
    <header className="fixed top-0 left-0 right-0 z-50 w-full border-b border-default bg-canvas/90 backdrop-blur-md">
      <div className="mx-auto flex h-14 max-w-6xl items-center justify-between px-4 sm:px-6">
        <Link to="/" className="flex items-center gap-2">
          <div className="flex h-7 w-7 items-center justify-center rounded bg-accent text-white">
            <Activity className="h-4 w-4" strokeWidth={2.25} />
          </div>
          <span className="font-display text-sm font-semibold text-main">
            SEOPulse
          </span>
        </Link>

        <div className="flex items-center gap-2">
          <Link
            to="/"
            className="px-2 py-1 text-sm text-muted transition-colors hover:text-main"
          >
            Home
          </Link>
          <Link
            to="/dashboard"
            className="inline-flex h-8 items-center rounded bg-accent px-3 text-sm font-medium text-white hover:bg-accent-hover"
          >
            Open app
          </Link>
        </div>
      </div>
    </header>
  )
}
