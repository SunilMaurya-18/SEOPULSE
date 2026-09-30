import { useMemo, useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom'

import { getErrorMessage } from '@/api/errors'
import { Logo } from '@/components/brand/Logo'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { useAuth } from '@/lib/auth'
import {
  peekPendingWebsiteUrl,
  setPendingWebsiteUrl,
} from '@/lib/pendingWebsite'
import { useToast } from '@/lib/toast'

export function LoginPage() {
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const location = useLocation()
  const { pushToast } = useToast()
  const from = (location.state as { from?: { pathname?: string; search?: string } } | null)?.from
  const returnTo = from?.pathname ? `${from.pathname}${from.search ?? ''}` : '/dashboard'

  const pendingFromQuery = useMemo(() => {
    const raw = searchParams.get('url') ?? searchParams.get('audit')
    return raw?.trim() || peekPendingWebsiteUrl()
  }, [searchParams])

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  if (isAuthenticated) {
    if (pendingFromQuery) {
      setPendingWebsiteUrl(pendingFromQuery)
    }
    return <Navigate to={pendingFromQuery ? '/dashboard' : returnTo} replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      if (pendingFromQuery) {
        setPendingWebsiteUrl(pendingFromQuery)
      }
      await login(email.trim(), password)
      pushToast({
        tone: 'success',
        title: 'Signed in',
        description: pendingFromQuery
          ? 'Connecting your website and starting an audit…'
          : 'Welcome back to SEOPulse.',
      })
      navigate(pendingFromQuery ? '/dashboard' : returnTo, { replace: true })
    } catch (err) {
      setError(getErrorMessage(err, 'Invalid email or password. Please try again.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-canvas px-4">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center">
          <Logo className="justify-center" />
          <h1 className="mt-6 font-display text-2xl font-semibold text-main">
            Sign in
          </h1>
          <p className="mt-1 text-sm text-muted">
            Access your SEO audit workspace.
          </p>
          {pendingFromQuery && (
            <p className="mt-3 rounded border border-default bg-surface px-3 py-2 font-mono text-xs text-muted">
              After sign-in we will connect{' '}
              <span className="text-main">{pendingFromQuery}</span>
            </p>
          )}
        </div>

        <form
          onSubmit={handleSubmit}
          className="space-y-4 rounded-lg border border-default bg-surface p-5"
        >
          {error && (
            <Alert variant="error" title="Sign in failed">
              {error}
            </Alert>
          )}

          <Input
            id="email"
            label="Email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@company.com"
          />

          <Input
            id="password"
            label="Password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
          />

          <div className="flex justify-end">
            <Link
              to="/forgot-password"
              className="text-xs font-medium text-accent hover:text-accent-hover"
            >
              Forgot password?
            </Link>
          </div>

          <Button type="submit" className="w-full" loading={loading}>
            Sign in
          </Button>
        </form>

        <p className="text-center text-sm text-muted">
          No account?{' '}
          <Link
            to={
              pendingFromQuery
                ? `/register?url=${encodeURIComponent(pendingFromQuery)}`
                : '/register'
            }
            className="font-medium text-accent hover:text-accent-hover"
          >
            Create one
          </Link>
        </p>
      </div>
    </div>
  )
}
