import { Link } from 'react-router-dom'

import { Logo } from '@/components/brand/Logo'

export function TermsHeader() {
  return (
    <header className="fixed top-0 right-0 left-0 z-50 w-full border-b border-default bg-canvas/90 backdrop-blur-md">
      <div className="mx-auto flex h-14 max-w-6xl items-center justify-between px-4 sm:px-6">
        <Logo />
        <div className="flex items-center gap-2">
          <Link to="/" className="px-2 py-1 font-mono text-sm text-muted hover:text-main">
            Home
          </Link>
          <Link
            to="/dashboard"
            className="inline-flex h-8 items-center rounded-md bg-accent px-3 font-mono text-sm text-on-accent hover:brightness-110"
          >
            Open app
          </Link>
        </div>
      </div>
    </header>
  )
}
