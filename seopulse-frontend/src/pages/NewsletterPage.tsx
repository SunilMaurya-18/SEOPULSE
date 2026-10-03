import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router-dom'

import { getErrorMessage } from '@/api/errors'
import { newsletterApi } from '@/api/newsletter'
import { MarketingHeader } from '@/features/landing/components/MarketingHeader'

type Status = 'working' | 'done' | 'error'

const copy = {
  confirm: {
    working: 'Confirming your subscription…',
    done: 'You are subscribed',
    detail: 'Thanks for confirming. We will only write when we have something worth reading.',
  },
  unsubscribe: {
    working: 'Unsubscribing…',
    done: 'You are unsubscribed',
    detail: 'You will not get any more mailing list emails from us. Account emails are not affected.',
  },
}

export function NewsletterPage() {
  const { pathname } = useLocation()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const action = pathname.endsWith('/unsubscribe') ? 'unsubscribe' : 'confirm'
  const [status, setStatus] = useState<Status>(token ? 'working' : 'error')
  const [error, setError] = useState(token ? '' : 'This link is missing its token.')
  const handled = useRef<string | null>(null)

  useEffect(() => {
    if (!token || handled.current === token) return
    handled.current = token
    newsletterApi[action](token)
      .then(() => setStatus('done'))
      .catch((err) => {
        setError(getErrorMessage(err, 'This link is invalid or has expired.'))
        setStatus('error')
      })
  }, [action, token])

  const text = copy[action]

  return (
    <div className="marketing dark min-h-dvh bg-black text-main">
      <MarketingHeader />
      <main className="mx-auto flex min-h-dvh max-w-xl flex-col justify-center px-5 py-32 md:px-[60px]">
        <p className="font-mono text-xs tracking-[0.22em] text-accent uppercase">Mailing list</p>
        <h1 className="mt-3 font-mono text-3xl leading-tight font-semibold tracking-tight sm:text-4xl" aria-live="polite">
          {status === 'working' ? text.working : status === 'done' ? text.done : 'Something went wrong'}
        </h1>
        {status !== 'working' && (
          <p className="mt-4 font-mono text-sm leading-relaxed text-muted">
            {status === 'done' ? text.detail : error}
          </p>
        )}
        <Link
          to="/"
          className="mt-10 inline-flex w-fit rounded-md bg-accent px-4 py-2.5 font-mono text-sm text-on-accent transition hover:-translate-y-0.5 hover:brightness-110"
        >
          Back to SEOPulse
        </Link>
      </main>
    </div>
  )
}
