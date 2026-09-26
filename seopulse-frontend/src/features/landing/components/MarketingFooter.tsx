import { Activity } from 'lucide-react'
import { Link } from 'react-router-dom'

export function MarketingFooter() {
  return (
    <footer className="border-t border-default bg-canvas">
      <div className="mx-auto flex w-full max-w-6xl flex-col items-start justify-between gap-3 px-4 py-6 sm:flex-row sm:items-center sm:px-6">
        <div className="flex items-center gap-2">
          <div className="flex h-6 w-6 items-center justify-center rounded bg-accent text-white">
            <Activity className="h-3.5 w-3.5" strokeWidth={2.25} />
          </div>
          <span className="font-display text-sm font-semibold text-main">
            SEOPulse
          </span>
        </div>

        <div className="flex items-center gap-4 font-mono text-[11px] text-muted">
          <Link to="/login" className="hover:text-main">
            Sign in
          </Link>
          <Link to="/terms" className="hover:text-main">
            Terms
          </Link>
          <span>© {new Date().getFullYear()} SEOPulse</span>
        </div>
      </div>
    </footer>
  )
}
