import { Link } from 'react-router-dom'

export function TermsFooter() {
  return (
    <footer className="border-t border-default bg-canvas">
      <div className="mx-auto flex w-full max-w-6xl flex-col items-start justify-between gap-3 px-4 py-6 sm:flex-row sm:items-center sm:px-6">
        <p className="font-mono text-[11px] text-muted">
          © {new Date().getFullYear()} SEOPulse
        </p>
        <div className="flex gap-4 font-mono text-[11px] text-muted">
          <Link to="/" className="hover:text-main">
            Home
          </Link>
          <Link to="/dashboard" className="hover:text-main">
            App
          </Link>
        </div>
      </div>
    </footer>
  )
}
