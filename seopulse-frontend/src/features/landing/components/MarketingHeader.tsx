import { Activity } from 'lucide-react'
import { Link } from 'react-router-dom'

import { useAuth } from '@/lib/auth'

export function MarketingHeader() {
  const { isAuthenticated } = useAuth()

  return (
    <header className="fixed top-0 left-0 z-50 w-full border-b border-default bg-canvas/90 backdrop-blur-md">
      <div className="mx-auto flex h-14 w-full max-w-5xl items-center justify-between px-4 sm:px-6">
        <Link to="/" className="flex items-center gap-2">
          <div className="flex h-7 w-7 items-center justify-center rounded bg-accent text-white">
            <Activity className="h-4 w-4" strokeWidth={2.25} />
          </div>
          <span className="font-display text-sm font-semibold tracking-tight text-main">
            SEOPulse
          </span>
        </Link>

        <div className="flex items-center gap-2">
          {isAuthenticated ? (
            <Link
              to="/dashboard"
              className="inline-flex h-8 items-center rounded bg-accent px-3 text-sm font-medium text-white hover:bg-accent-hover"
            >
              Open app
            </Link>
          ) : (
            <>
              <Link
                to="/login"
                className="px-2 py-1 text-sm text-muted transition-colors hover:text-main"
              >
                Sign in
              </Link>
              <Link
                to="/register"
                className="inline-flex h-8 items-center rounded bg-accent px-3 text-sm font-medium text-white hover:bg-accent-hover"
              >
                Get started
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
