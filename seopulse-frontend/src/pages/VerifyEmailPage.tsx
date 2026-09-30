import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'

import { authApi } from '@/api/auth'
import { getErrorMessage } from '@/api/errors'
import { AuthShell } from '@/components/auth/AuthShell'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { useAuth } from '@/lib/auth'

type State = { status: 'verifying' } | { status: 'verified' } | { status: 'failed'; message: string }

export function VerifyEmailPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [token] = useState(() => searchParams.get('token') ?? '')
  const { isAuthenticated, markEmailVerified } = useAuth()
  const [state, setState] = useState<State>(
    token
      ? { status: 'verifying' }
      : { status: 'failed', message: 'This verification link is missing its token.' },
  )
  // Tokens are single-use, so StrictMode's double effect must not submit twice.
  const submitted = useRef(false)

  useEffect(() => {
    if (searchParams.has('token')) setSearchParams({}, { replace: true })
  }, [searchParams, setSearchParams])

  useEffect(() => {
    if (!token || submitted.current) return
    submitted.current = true
    authApi
      .verifyEmail(token)
      .then(() => {
        markEmailVerified()
        setState({ status: 'verified' })
      })
      .catch((err) =>
        setState({
          status: 'failed',
          message: getErrorMessage(err, 'This verification link is invalid or has expired.'),
        }),
      )
  }, [token, markEmailVerified])

  const next = isAuthenticated ? '/dashboard' : '/login'

  return (
    <AuthShell title="Verify your email">
      {state.status === 'verifying' && (
        <p className="text-center text-sm text-muted">Verifying your email address…</p>
      )}
      {state.status === 'verified' && (
        <>
          <Alert variant="success" title="Email verified">
            You can now run SEO audits.
          </Alert>
          <Link to={next}>
            <Button className="w-full">
              {isAuthenticated ? 'Go to dashboard' : 'Sign in'}
            </Button>
          </Link>
        </>
      )}
      {state.status === 'failed' && (
        <>
          <Alert variant="error" title="Verification failed">
            {state.message}
          </Alert>
          <p className="text-sm text-muted">
            Signed-in users can request a new link from the banner at the top of the workspace.
          </p>
          <Link to={next}>
            <Button variant="secondary" className="w-full">
              {isAuthenticated ? 'Back to dashboard' : 'Sign in'}
            </Button>
          </Link>
        </>
      )}
    </AuthShell>
  )
}
