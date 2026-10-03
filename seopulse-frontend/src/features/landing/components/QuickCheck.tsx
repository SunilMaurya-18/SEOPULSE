import { useState, type FormEvent } from 'react'
import { ArrowRight, Loader2 } from 'lucide-react'
import { Link } from 'react-router-dom'

import { getErrorMessage } from '@/api/errors'
import { quickCheckApi, type QuickCheckIssue, type QuickCheckResult } from '@/api/quickCheck'
import { captchaEnabled, Turnstile } from '@/components/security/Turnstile'
import { ScoreRing } from '@/components/ui/ScoreRing'
import { cn } from '@/lib/cn'

const categoryLabels: Record<string, string> = {
  CONTENT: 'Content',
  TECHNICAL: 'Technical',
  LINKS: 'Links',
  SOCIAL: 'Social',
  PERFORMANCE: 'Performance',
  SECURITY: 'Security',
}

const severityStyles: Record<QuickCheckIssue['severity'], { label: string; dot: string }> = {
  ERROR: { label: 'Error', dot: 'bg-[#ff375f]' },
  WARNING: { label: 'Warning', dot: 'bg-[#ffd60a]' },
  INFO: { label: 'Notice', dot: 'bg-[#64d2ff]' },
}

function hostOf(url: string) {
  try {
    return new URL(url).hostname.replace(/^www\./, '')
  } catch {
    return url
  }
}

function barColor(score: number) {
  if (score >= 80) return 'bg-[#30d158]'
  if (score >= 60) return 'bg-[#ffd60a]'
  return 'bg-[#ff375f]'
}

export function QuickCheck() {
  const [url, setUrl] = useState('')
  const [checking, setChecking] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState<QuickCheckResult | null>(null)
  const [captchaToken, setCaptchaToken] = useState<string | null>(null)
  const [captchaKey, setCaptchaKey] = useState(0)

  async function submit(event: FormEvent) {
    event.preventDefault()
    const trimmed = url.trim()
    if (!trimmed) {
      setError('Enter your website address.')
      return
    }
    if (captchaEnabled && !captchaToken) {
      setError('Complete the security check below, then try again.')
      return
    }
    setError('')
    setResult(null)
    setChecking(true)
    try {
      setResult(await quickCheckApi.run(trimmed, captchaToken ?? undefined))
    } catch (err) {
      setError(getErrorMessage(err, 'We could not check that site. Please try again.'))
    } finally {
      setChecking(false)
      setCaptchaKey((key) => key + 1)
    }
  }

  return (
    <div className="w-full max-w-[720px]">
      <form onSubmit={(event) => void submit(event)} className="flex flex-col gap-3 sm:flex-row" noValidate>
        <label htmlFor="quick-check-url" className="sr-only">
          Website address
        </label>
        <input
          id="quick-check-url"
          type="text"
          inputMode="url"
          autoComplete="url"
          spellCheck={false}
          value={url}
          onChange={(event) => setUrl(event.target.value)}
          placeholder="yourwebsite.com"
          maxLength={2048}
          className="h-12 w-full flex-1 rounded-md border border-white/20 bg-black/40 px-4 font-mono text-sm text-[#f2f5ea] outline-none backdrop-blur-sm transition placeholder:text-[#f2f5ea]/40 focus:border-accent"
        />
        <button
          type="submit"
          disabled={checking}
          className="inline-flex h-12 items-center justify-center gap-2 rounded-md bg-accent px-5 font-mono text-sm text-on-accent transition hover:-translate-y-0.5 hover:brightness-110 disabled:pointer-events-none disabled:opacity-70"
        >
          {checking ? <Loader2 className="h-4 w-4 animate-spin" aria-hidden /> : null}
          {checking ? 'Checking…' : 'Check my site free'}
        </button>
      </form>

      <Turnstile key={captchaKey} theme="dark" onToken={setCaptchaToken} className="mt-3" />

      <p className="mt-2 font-mono text-xs text-[#8b93a1]" aria-live="polite">
        {checking ? 'Crawling a few pages. This takes up to 30 seconds.' : 'No sign-up needed. We check up to 5 pages.'}
      </p>

      {error && (
        <p role="alert" className="mt-3 font-mono text-sm text-accent">
          {error}
        </p>
      )}

      {result && <QuickCheckReport result={result} />}
    </div>
  )
}

function QuickCheckReport({ result }: { result: QuickCheckResult }) {
  const hidden = result.issueTypes - result.issues.length
  const categories = Object.entries(result.categoryScores)

  return (
    <section
      aria-label={`SEO check for ${hostOf(result.url)}`}
      className="animate-page-enter mt-6 rounded-2xl border border-white/10 bg-[#0c1016]/90 p-5 text-left backdrop-blur-md sm:p-6"
    >
      <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
        <ScoreRing score={result.score} size={104} strokeWidth={9} />
        <div className="min-w-0 flex-1">
          <p className="font-mono text-xs tracking-[0.18em] text-accent uppercase">Quick check</p>
          <h2 className="mt-1 truncate font-mono text-xl font-semibold tracking-tight text-[#f2f5ea]">
            {hostOf(result.url)}
          </h2>
          <p className="mt-1 font-mono text-xs text-[#8b93a1]">
            {result.pagesChecked} {result.pagesChecked === 1 ? 'page' : 'pages'} checked · {result.errorCount} errors ·{' '}
            {result.warningCount} warnings
            {result.partial ? ' · partial, the site responded slowly' : ''}
          </p>
        </div>
      </div>

      {categories.length > 0 && (
        <ul className="mt-5 grid grid-cols-2 gap-x-5 gap-y-3 sm:grid-cols-3">
          {categories.map(([category, score]) => (
            <li key={category}>
              <div className="flex items-center justify-between font-mono text-xs text-[#c9cfd8]">
                <span>{categoryLabels[category] ?? category}</span>
                <span className="text-[#f2f5ea]">{score}</span>
              </div>
              <div className="mt-1.5 h-1 overflow-hidden rounded-full bg-white/10">
                <div className={cn('h-full rounded-full', barColor(score))} style={{ width: `${score}%` }} />
              </div>
            </li>
          ))}
        </ul>
      )}

      {result.issues.length > 0 ? (
        <ul className="mt-6 divide-y divide-white/10 border-t border-white/10">
          {result.issues.map((issue) => {
            const severity = severityStyles[issue.severity] ?? severityStyles.INFO
            return (
              <li key={issue.ruleCode} className="py-3">
                <div className="flex items-start gap-3">
                  <span className={cn('mt-1.5 h-2 w-2 shrink-0 rounded-full', severity.dot)} aria-hidden />
                  <div className="min-w-0">
                    <p className="font-mono text-sm text-[#f2f5ea]">
                      {issue.title}
                      <span className="ml-2 text-xs text-[#8b93a1]">
                        {severity.label} · {issue.pages} {issue.pages === 1 ? 'page' : 'pages'}
                      </span>
                    </p>
                    <p className="mt-1 text-[13px] leading-relaxed text-[#8b93a1]">{issue.recommendation}</p>
                  </div>
                </div>
              </li>
            )
          })}
        </ul>
      ) : (
        <p className="mt-6 border-t border-white/10 pt-4 font-mono text-sm text-[#c9cfd8]">
          No issues on the pages we checked. A full audit covers every page.
        </p>
      )}

      <div className="mt-5 flex flex-col gap-3 border-t border-white/10 pt-5 sm:flex-row sm:items-center sm:justify-between">
        <p className="font-mono text-xs leading-relaxed text-[#8b93a1]">
          {hidden > 0 ? `${hidden} more issue ${hidden === 1 ? 'type' : 'types'} found. ` : ''}
          The free plan audits up to 100 pages with a downloadable report.
        </p>
        <Link
          to={`/register?url=${encodeURIComponent(result.url)}`}
          className="inline-flex shrink-0 items-center justify-center gap-2 rounded-md bg-accent px-4 py-2.5 font-mono text-sm text-on-accent transition hover:-translate-y-0.5 hover:brightness-110"
        >
          Get the full report
          <ArrowRight className="h-4 w-4" aria-hidden />
        </Link>
      </div>
    </section>
  )
}
