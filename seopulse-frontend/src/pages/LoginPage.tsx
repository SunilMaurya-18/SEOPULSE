import { useMemo, useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom'

import { getErrorMessage } from '@/api/errors'
import { AuthShell, PendingWebsiteNotice } from '@/components/auth/AuthShell'
import { GoogleSignInButton } from '@/components/auth/GoogleSignInButton'
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
  const { login, loginWithGoogle, isAuthenticated } = useAuth()
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

  async function handleGoogle(credential: string) {
    setError(null)
    setLoading(true)
    try {
      if (pendingFromQuery) {
        setPendingWebsiteUrl(pendingFromQuery)
      }
      await loginWithGoogle(credential)
      pushToast({ tone: 'success', title: 'Signed in', description: 'Welcome to SEOPulse.' })
      navigate(pendingFromQuery ? '/dashboard' : returnTo, { replace: true })
    } catch (err) {
      setError(getErrorMessage(err, 'Google sign-in failed. Please try again.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell
      title="Welcome back"
      description="Sign in to your SEO audit workspace."
      notice={
        pendingFromQuery && <PendingWebsiteNotice url={pendingFromQuery} action="After you sign in," />
      }
      footer={
        <>
          New to SEOPulse?{' '}
          <Link
            to={
              pendingFromQuery
                ? `/register?url=${encodeURIComponent(pendingFromQuery)}`
                : '/register'
            }
            className="font-semibold text-accent hover:text-accent-hover"
          >
            Create an account
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <Alert variant="error" title="Sign in failed">
            {error}
          </Alert>
        )}

        <GoogleSignInButton text="signin_with" onCredential={(credential) => void handleGoogle(credential)} />

        <Input
          id="email"
          label="Email"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="you@company.com"
          className="h-11"
        />

        <div className="space-y-2">
          <Input
            id="password"
            label="Password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Your password"
            className="h-11"
          />
          <div className="flex justify-end">
            <Link
              to="/forgot-password"
              className="text-[13px] font-medium text-accent hover:text-accent-hover"
            >
              Forgot password?
            </Link>
          </div>
        </div>

        <Button type="submit" size="lg" className="w-full" loading={loading}>
          Sign in
        </Button>
      </form>
    </AuthShell>
  )
}
