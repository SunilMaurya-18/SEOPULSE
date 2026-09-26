import { Link } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'

import { useAuth } from '@/lib/auth'

export function FinalCtaSection() {
  const { isAuthenticated } = useAuth()

  return (
    <section className="px-4 py-20 sm:px-6">
      <div className="mx-auto w-full max-w-5xl">
        <h2 className="font-display text-3xl font-semibold tracking-tight text-main sm:text-4xl">
          Ready to pulse your site?
        </h2>
        <p className="mt-3 max-w-xl text-base text-muted">
          Create a workspace, connect a domain, and run your first crawl in
          minutes.
        </p>
        <div className="mt-8 flex flex-wrap gap-3">
          <Link
            to={isAuthenticated ? '/dashboard' : '/register'}
            className="inline-flex h-11 items-center gap-1.5 rounded bg-accent px-5 text-sm font-medium text-white hover:bg-accent-hover"
          >
            {isAuthenticated ? 'Open dashboard' : 'Create account'}
            <ArrowRight className="h-4 w-4" />
          </Link>
          {!isAuthenticated && (
            <Link
              to="/login"
              className="inline-flex h-11 items-center rounded border border-default bg-surface px-5 text-sm font-medium text-main hover:bg-surface-elevated"
            >
              Sign in
            </Link>
          )}
        </div>
      </div>
    </section>
  )
}
