import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowRight, Globe } from 'lucide-react'

import { setPendingWebsiteUrl } from '@/lib/pendingWebsite'
import { useAuth } from '@/lib/auth'

export function HeroSection() {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()
  const [url, setUrl] = useState('')

  function runQuickAudit(event: FormEvent) {
    event.preventDefault()
    const cleaned = url.trim()
    if (!cleaned) {
      navigate(isAuthenticated ? '/dashboard' : '/register')
      return
    }

    setPendingWebsiteUrl(cleaned)

    if (isAuthenticated) {
      navigate('/dashboard')
      return
    }

    navigate(`/register?url=${encodeURIComponent(cleaned)}`)
  }

  return (
    <section className="relative isolate overflow-hidden border-b border-default">
      <div
        className="animate-land-pan absolute inset-0 -z-10 bg-[length:160%_160%] bg-[linear-gradient(125deg,#f8fafc_0%,#fee2e2_28%,#f8fafc_52%,#e2e8f0_78%,#f8fafc_100%)] dark:bg-[linear-gradient(125deg,#09090b_0%,#3f1212_30%,#09090b_55%,#1c1b1d_80%,#09090b_100%)]"
        aria-hidden
      />
      <div
        className="absolute inset-0 -z-10 opacity-[0.35] dark:opacity-[0.25]"
        style={{
          backgroundImage:
            'radial-gradient(circle at 1px 1px, color-mix(in oklab, var(--sp-main) 18%, transparent) 1px, transparent 0)',
          backgroundSize: '22px 22px',
        }}
        aria-hidden
      />

      <div className="mx-auto flex min-h-[calc(100vh-3.5rem)] w-full max-w-5xl flex-col justify-center px-4 py-16 sm:px-6 sm:py-20">
        <p className="animate-land-rise font-display text-5xl font-semibold tracking-tight text-main sm:text-7xl md:text-8xl">
          SEOPulse
        </p>
        <div className="animate-land-line mt-5 h-1 w-44 rounded-full bg-accent" />

        <h1 className="animate-land-rise-delay mt-8 max-w-2xl font-display text-2xl font-semibold tracking-tight text-main sm:text-4xl">
          See what search engines see — then fix it.
        </h1>
        <p className="animate-land-rise-late mt-4 max-w-xl text-base leading-7 text-muted sm:text-lg">
          Crawl your site, score SEO health, and ship clearer fixes from one
          premium workspace.
        </p>

        <form
          onSubmit={runQuickAudit}
          className="animate-land-rise-late mt-10 flex w-full max-w-xl flex-col gap-2 sm:flex-row"
        >
          <div className="flex min-w-0 flex-1 items-center gap-2 rounded-xl border border-default bg-surface/90 px-3 shadow-overlay backdrop-blur">
            <Globe className="h-4 w-4 shrink-0 text-dim" />
            <input
              aria-label="Website URL"
              className="w-full bg-transparent py-3.5 font-mono text-sm text-main placeholder:text-dim focus:outline-none"
              placeholder="example.com"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
            />
          </div>
          <button
            type="submit"
            className="inline-flex h-12 items-center justify-center gap-1.5 rounded-xl bg-accent px-5 text-sm font-medium text-white shadow-[0_16px_30px_-18px_var(--sp-accent)] transition hover:bg-accent-hover"
          >
            Start free audit
            <ArrowRight className="h-4 w-4" />
          </button>
        </form>

        <p className="animate-land-rise-late mt-4 font-mono text-[11px] tracking-wide text-dim uppercase">
          Connect · Crawl · Analyze · Report
        </p>
      </div>
    </section>
  )
}
